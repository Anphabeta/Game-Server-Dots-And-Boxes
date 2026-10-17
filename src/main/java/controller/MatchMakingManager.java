package controller;

import dao.MatchDAO;
import dao.UserDAO;
import game.Room;
import game.RoomManager;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import model.Match;
import model.User;
import request.Encoder;

public class MatchMakingManager {
    public static final String MODE_ANY = "ANY";
    public static final String SIZE_3X3 = "3x3";
    public static final String SIZE_4X4 = "4x4";
    public static final String SIZE_5X5 = "5x5";

    public static final String[] SUPPORTED_SIZES = { SIZE_3X3, SIZE_4X4, SIZE_5X5 };

    // Map chứa các hàng đợi riêng cho từng kích thước bàn (3x3, 4x4, 5x5)
    private final Map<String, ConcurrentLinkedQueue<UserRepresentative>> queuesByBoardSize = new ConcurrentHashMap<>();
    
    // Map tra cứu nhanh người chơi đang ở hàng đợi của kích thước nào (O(1) cho cleanup)
    private final Map<UserRepresentative, String> playerCurrentSize = new ConcurrentHashMap<>();

    private final Random random = new Random();

    private Encoder encoder;
    private RoomManager roomManager;
    private UserDAO userDAO;
    private MatchDAO matchDAO;

    public MatchMakingManager() {
        for (String size : SUPPORTED_SIZES) {
            queuesByBoardSize.put(size, new ConcurrentLinkedQueue<>());
        }
    }

    public void setEncoder(Encoder encoder) {
        this.encoder = encoder;
    }

    public void setRoomManager(RoomManager roomManager) {
        this.roomManager = roomManager;
    }

    public void setDAO(UserDAO userDAO, MatchDAO matchDAO) {
        this.userDAO = userDAO;
        this.matchDAO = matchDAO;
    }

    /**
     * Phân giải requestedMode thành kích thước bàn cờ cụ thể (3x3, 4x4 hoặc 5x5).<br>
     * - Nếu là 3x3, 4x4, 5x5: Trả về chính size đó.<br>
     * - Nếu là ANY (hoặc null/rỗng):<br>
     *   + Ưu tiên 1 (Smart Pick): Chọn queue đang có số người chờ lẻ (đang có người chờ ghép đối thủ).<br>
     *   + Ưu tiên 2: Random ngẫu nhiên 1 trong 3 kích thước nếu tất cả đều trống hoặc chẵn.<br>
     * @param requestedMode Chế độ người chơi chọn ("ANY", "3x3", "4x4", "5x5")
     * @return Kích thước cụ thể ("3x3", "4x4", hoặc "5x5")
     */
    public String resolveBoardSize(String requestedMode) {
        if (requestedMode == null || requestedMode.trim().isEmpty()) {
            requestedMode = MODE_ANY;
        }
        requestedMode = requestedMode.trim().toUpperCase();

        // 1. Nếu người chơi yêu cầu một kích thước cụ thể hợp lệ
        for (String size : SUPPORTED_SIZES) {
            if (size.equalsIgnoreCase(requestedMode)) {
                return size;
            }
        }

        // 2. Nếu người chơi chọn ANY (hoặc param không xác định)
        // Ưu tiên 1 (Smart Pick): Tìm hàng đợi nào đang có người chơi thực sự active đang chờ lẻ
        for (String size : SUPPORTED_SIZES) {
            ConcurrentLinkedQueue<UserRepresentative> q = queuesByBoardSize.get(size);
            if (q != null && !q.isEmpty()) {
                // Kiểm tra xem người đứng đầu queue có thực sự còn kết nối không
                UserRepresentative waitingPlayer = q.peek();
                if (waitingPlayer != null && isPlayerActive(waitingPlayer) && (q.size() % 2 != 0)) {
                    System.out.println(String.format("SmartMatch: Found active waiting player in queue [%s]! Matched into this size.", size));
                    return size;
                }
            }
        }

        // Ưu tiên 2: Nếu không có ai đang chờ hoặc tất cả đều chẵn: Random ngẫu nhiên 1 trong 3
        int randomIndex = random.nextInt(SUPPORTED_SIZES.length);
        String chosenSize = SUPPORTED_SIZES[randomIndex];
        System.out.println(String.format("SmartMatch: All queues empty or balanced. Randomly chosen [%s].", chosenSize));
        return chosenSize;
    }

    /**
     * Lấy kích thước bàn cờ mà người chơi đang được xếp hàng (nếu có).
     */
    public String getAssignedSize(UserRepresentative player) {
        return playerCurrentSize.get(player);
    }

    /**
     * Thêm người chơi vào hàng đợi tìm trận theo chế độ yêu cầu.<br>
     * Luồng chính:<br>
     * - Phân giải requestedMode thành targetSize cụ thể.<br>
     * - Thêm player vào hàng đợi của targetSize.<br>
     * - Thử ghép trận cho targetSize.<br>
     * @param player Đại diện của người chơi
     * @param requestedMode Chế độ kích thước ("ANY", "3x3", "4x4", "5x5")
     * @return true nếu thêm thành công, false nếu đã có trong hàng đợi
     */
    public synchronized boolean addPlayer(UserRepresentative player, String requestedMode) {
        if (player == null) {
            return false;
        }

        // Nếu người chơi đã nằm trong bất kỳ hàng đợi nào -> từ chối
        if (playerCurrentSize.containsKey(player)) {
            return false;
        }

        // Phân giải mode thành size cụ thể
        String targetSize = resolveBoardSize(requestedMode);

        ConcurrentLinkedQueue<UserRepresentative> queue = queuesByBoardSize.get(targetSize);
        if (queue == null) {
            return false;
        }

        queue.add(player);
        playerCurrentSize.put(player, targetSize);

        // Cập nhật trạng thái QUEUING vào Database
        if (this.userDAO != null) {
            this.userDAO.updateUserStatus(player.user.getUserId(), User.STATUS.QUEUING);
        }

        System.out.println(String.format("Player %s (Session %d) joined quick match queue [%s]. Current queue size: %d",
                player.user.getUsername(), player.sessionId, targetSize, queue.size()));

        tryMatch(targetSize);
        return true;
    }

    /**
     * Backward-compatibility: Thêm người chơi mặc định chế độ ANY.
     */
    public synchronized boolean addPlayer(UserRepresentative player) {
        return addPlayer(player, MODE_ANY);
    }

    /**
     * Rút người chơi khỏi hàng đợi tìm trận (do hủy tìm trận hoặc disconnect).<br>
     * Luồng hoạt động chính:<br>
     * - Xác định kích thước mà người chơi đang chờ qua playerCurrentSize.<br>
     * - Xóa người chơi khỏi hàng đợi tương ứng với O(1).<br>
     * - Đặt lại trạng thái QUEUING -> ONLINE (cả in-memory và Database).<br>
     * @param player Đại diện của người chơi
     * @return true nếu người chơi có trong hàng đợi và đã được xóa
     */
    public synchronized boolean removePlayer(UserRepresentative player) {
        if (player == null) {
            return false;
        }

        String targetSize = playerCurrentSize.remove(player);
        if (targetSize != null) {
            ConcurrentLinkedQueue<UserRepresentative> queue = queuesByBoardSize.get(targetSize);
            if (queue != null) {
                queue.remove(player);
            }

            System.out.println(String.format("Player %s (Session %d) removed from quick match queue [%s].",
                    player.user.getUsername(), player.sessionId, targetSize));

            if (User.STATUS.QUEUING.equalsIgnoreCase(player.user.getStatus())) {
                player.user.setStatus(User.STATUS.ONLINE);
            }

            // Đồng bộ trạng thái ONLINE vào Database
            if (this.userDAO != null) {
                this.userDAO.updateUserStatus(player.user.getUserId(), User.STATUS.ONLINE);
            }
            return true;
        }
        return false;
    }

    /**
     * Thử ghép cặp nếu có ít nhất 2 người trong hàng đợi của targetSize.<br>
     * @param targetSize Kích thước bàn cờ cần ghép ("3x3", "4x4", "5x5")
     */
    private synchronized void tryMatch(String targetSize) {
        ConcurrentLinkedQueue<UserRepresentative> queue = queuesByBoardSize.get(targetSize);
        if (queue == null) {
            return;
        }

        while (queue.size() >= 2) {
            UserRepresentative player1 = queue.poll();
            UserRepresentative player2 = queue.poll();

            // Kiểm tra tính hợp lệ (còn kết nối) của player 1
            if (player1 == null || !isPlayerActive(player1)) {
                if (player1 != null) playerCurrentSize.remove(player1);
                if (player2 != null && isPlayerActive(player2)) {
                    queue.add(player2); // Đưa player 2 trở lại queue
                } else if (player2 != null) {
                    playerCurrentSize.remove(player2);
                }
                continue;
            }

            // Kiểm tra tính hợp lệ của player 2
            if (player2 == null || !isPlayerActive(player2)) {
                if (player2 != null) playerCurrentSize.remove(player2);
                if (isPlayerActive(player1)) {
                    queue.add(player1); // Đưa player 1 trở lại queue
                } else {
                    playerCurrentSize.remove(player1);
                }
                continue;
            }

            // Cả hai người đều hợp lệ -> Xóa khỏi playerCurrentSize và khởi tạo trận đấu
            playerCurrentSize.remove(player1);
            playerCurrentSize.remove(player2);

            createMatch(player1, player2, targetSize);
        }
    }

    /**
     * Kiểm tra kết nối của người chơi còn hoạt động hay không.
     */
    private boolean isPlayerActive(UserRepresentative player) {
        return player.state != null && player.state.isValid();
    }

    /**
     * Trích xuất kích thước lưới (ví dụ: "3x3" -> 3, "4x4" -> 4).
     */
    private int parseDimension(String sizeStr) {
        try {
            String[] parts = sizeStr.toLowerCase().split("x");
            return Integer.parseInt(parts[0]);
        } catch (Exception e) {
            return 3;
        }
    }

    /**
     * Xử lý khi ghép thành công 2 người chơi với kích thước bàn cờ cụ thể.<br>
     * Luồng xử lý:<br>
     * - Trích xuất broadSize và tạo phòng 2 người với broadSize đó<br>
     * - Tạo bản ghi Match mới trong DB gồm 2 userId, boardSize, matchStatus, startTime<br>
     * - Đổi trạng thái 2 người sang INGAME<br>
     * - Lưu trạng thái INGAME vào DB<br>
     * - Gửi thông báo MATCH_FOUND cho 2 client<br>
     */
    private void createMatch(UserRepresentative p1, UserRepresentative p2, String boardSize) {
        int dim = parseDimension(boardSize);
        RoomManager rm = this.roomManager != null ? this.roomManager : RoomManager.getInstance();
        Room room = rm.createRoom(p1, p2, dim, dim);
        int roomId = room.getRoomId();

        // Cung cấp DAO, Encoder và RoomManager cho Room để xử lý kết thúc ván đấu
        room.setDAO(this.userDAO, this.matchDAO);
        room.setEncoder(this.encoder);
        room.setRoomManager(rm);

        // Khởi tạo bản ghi Match trong Database ngay khi trận đấu bắt đầu
        if (this.matchDAO != null) {
            String boardSizeStr = room.getBoxRows() + "x" + room.getBoxCols();
            Match matchRecord = new Match(p1.user.getUserId(), p2.user.getUserId(), boardSizeStr);
            matchRecord.setMatchStatus("playing");
            matchRecord.setStartTime(new Timestamp(room.getStartTime()));
            int dbMatchId = this.matchDAO.createMatch(matchRecord);
            room.setMatchId(dbMatchId);
        }

        p1.setCurrentRoom(room);
        p2.setCurrentRoom(room);

        p1.user.setStatus(User.STATUS.INGAME);
        p2.user.setStatus(User.STATUS.INGAME);
        if (this.userDAO != null) {
            this.userDAO.updateUserStatus(p1.user.getUserId(), User.STATUS.INGAME);
            this.userDAO.updateUserStatus(p2.user.getUserId(), User.STATUS.INGAME);
        }

        System.out.println(String.format("Match created [%s]! Room: %d, Player 1: %s vs Player 2: %s",
                boardSize, roomId, p1.user.getUsername(), p2.user.getUsername()));

        if (encoder != null) {
            // Định dạng matchInfo: roomId;opponentUsername;opponentDisplayName;opponentRanking;role;boardSize
            // role = 1: Đi trước (màu đỏ), role = 2: Đi sau (màu xanh)
            String matchInfoForP1 = String.format("%d;%s;%s;%d;1;%s",
                    roomId,
                    p2.user.getUsername(),
                    p2.user.getDisplayName(),
                    p2.user.getRanking(),
                    boardSize);

            String matchInfoForP2 = String.format("%d;%s;%s;%d;2;%s",
                    roomId,
                    p1.user.getUsername(),
                    p1.user.getDisplayName(),
                    p1.user.getRanking(),
                    boardSize);

            encoder.notifyMatchFound(p1.state, matchInfoForP1);
            encoder.notifyMatchFound(p2.state, matchInfoForP2);
        }
    }

    public synchronized int getQueueSize(String boardSize) {
        ConcurrentLinkedQueue<UserRepresentative> q = queuesByBoardSize.get(boardSize);
        return q != null ? q.size() : 0;
    }

    public synchronized int getTotalQueueSize() {
        return playerCurrentSize.size();
    }
}

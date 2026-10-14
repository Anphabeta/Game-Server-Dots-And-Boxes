package controller;

import dao.MatchDAO;
import dao.UserDAO;
import game.Room;
import game.RoomManager;
import java.util.concurrent.ConcurrentLinkedQueue;
import model.User;
import request.Encoder;

public class MatchMakingManager {
    private final ConcurrentLinkedQueue<UserRepresentative> queue = new ConcurrentLinkedQueue<>();
    private Encoder encoder;
    private RoomManager roomManager;
    private UserDAO userDAO;
    private MatchDAO matchDAO;
    private static int roomCounter = 1;

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
     * Thêm người chơi vào hàng đợi tìm trận nhanh.<br>
     * Luồng chính:<br>
     * - Thêm player vào hàng đợi<br>
     * - Thử ghép trận<br>
     * Luồng ngoại lệ:<br>
     * - Nếu người chơi không tồn tại, hủy hàm<br>
     * - Nếu hàng đợi đã có người chơi, hủy hàm<br>
     * @param player Đại diện của người chơi
     * @return true nếu thêm thành công, false nếu đã có trong hàng đợi
     */
    public synchronized boolean addPlayer(UserRepresentative player) {
        if (player == null) {
            return false;
        }

        if (queue.contains(player)) {
            return false;
        }

        queue.add(player);
        System.out.println(String.format("Player %s (Session %d) joined quick match queue. Current queue size: %d",
                player.user.getUsername(), player.sessionId, queue.size()));

        tryMatch();
        return true;
    }

    /**
     * Rút người chơi khỏi hàng đợi tìm trận (do hủy tìm trận hoặc disconnect)<br>
     * Luồng hoạt động chính:<br>
     * - Xóa người chơi khỏi hàng đợi<br>
     * - Đặt lại trạng thái QUEUING -> ONLINE<br>
     * Luồng ngoại lệ:<br>
     * - Nếu người chơi không tồn tại thì hủy hàm<br>
     * @param player Đại diện của người chơi
     * @return true nếu người chơi có trong hàng đợi và đã được xóa
     */
    public synchronized boolean removePlayer(UserRepresentative player) {
        if (player == null) {
            return false;
        }

        boolean removed = queue.remove(player);
        if (removed) {
            System.out.println(String.format("Player %s (Session %d) removed from quick match queue. Remaining: %d",
                    player.user.getUsername(), player.sessionId, queue.size()));
            if (User.STATUS.QUEUING.equalsIgnoreCase(player.user.getStatus())) {
                player.user.setStatus(User.STATUS.ONLINE);
            }
        }
        return removed;
    }

    /**
     * Thử ghép cặp nếu có ít nhất 2 người trong hàng đợi<br>
     * Luồng chính:
     * - Nếu size >=2 lôi 2 người chơi khỏi hàng đợi<br>
     * - Tạo trận đấu với 2 người chơi đó<br>
     * Luồng ngoại lệ:<br>
     * - Nếu size bé hơn 2 thì hủy hàm<br>
     * - Nếu người 1 không kết nối, người 2 còn kết nối thì đưa người 2 trở lại hàng đợi<br>
     * - Nếu người 1 không kết nối, người 2 không kết nối thì thôi, bỏ ra khỏi hàng đợi luôn<br>
     * - Nếu người 1 kết nối, người 2 không kết nối thì đưa người 1 trở lại hàng đợi<br>
     * .
     */
    private synchronized void tryMatch() {
        while (queue.size() >= 2) {
            UserRepresentative player1 = queue.poll();
            UserRepresentative player2 = queue.poll();

            // Kiểm tra tính hợp lệ (còn kết nối) của player 1
            if (player1 == null || !isPlayerActive(player1)) {
                if (player2 != null && isPlayerActive(player2)) {
                    queue.add(player2); // Đưa player 2 trở lại queue
                }
                continue;
            }

            // Kiểm tra tính hợp lệ của player 2
            if (player2 == null || !isPlayerActive(player2)) {
                if (isPlayerActive(player1)) {
                    queue.add(player1); // Đưa player 1 trở lại queue
                }
                continue;
            }

            // Cả hai người đều hợp lệ -> Khởi tạo trận đấu
            createMatch(player1, player2);
        }
    }

    /**
     * Kiểm tra kết nối của người chơi còn hoạt động hay không<br>
     * Hợp lệ khi và chỉ khi ClientState còn tồn tại.
     */
    private boolean isPlayerActive(UserRepresentative player) {
        return player.state != null && player.state.isValid();
    }

    /**
     * Xử lý khi ghép thành công 2 người chơi<br>
     * Luồng chính:<br>
     * - Tăng id room<br>
     * - Đặt trạng thái 2 người chơi là INGAME<br>
     * - Gửi thông báo đã match thành công cho cả 2 client<br>
     * Luồng ngoại lệ:<br>
     * - Nếu không tồn tại encoder thì hủy hàm<br>
     * .
     */
    private void createMatch(UserRepresentative p1, UserRepresentative p2) {
        RoomManager rm = this.roomManager != null ? this.roomManager : RoomManager.getInstance();
        Room room = rm.createRoom(p1, p2, 3, 3);
        int roomId = room.getRoomId();

        // Cung cấp DAO, Encoder và RoomManager cho Room để xử lý kết thúc ván đấu
        room.setDAO(this.userDAO, this.matchDAO);
        room.setEncoder(this.encoder);
        room.setRoomManager(rm);

        // Khởi tạo bản ghi Match trong Database ngay khi trận đấu bắt đầu
        if (this.matchDAO != null) {
            model.Match matchRecord = new model.Match(p1.user.getUserId(), p2.user.getUserId());
            matchRecord.setMatchStatus("playing");
            matchRecord.setStartTime(new java.sql.Timestamp(room.getStartTime()));
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

        System.out.println(String.format("Match created! Room: %d, Player 1: %s vs Player 2: %s",
                roomId, p1.user.getUsername(), p2.user.getUsername()));

        if (encoder != null) {
            // Định dạng matchInfo: roomId;opponentUsername;opponentDisplayName;opponentRanking;role
            // role = 1: Đi trước (màu đỏ), role = 2: Đi sau (màu xanh)
            String matchInfoForP1 = String.format("%d;%s;%s;%d;1",
                    roomId,
                    p2.user.getUsername(),
                    p2.user.getDisplayName(),
                    p2.user.getRanking());

            String matchInfoForP2 = String.format("%d;%s;%s;%d;2",
                    roomId,
                    p1.user.getUsername(),
                    p1.user.getDisplayName(),
                    p1.user.getRanking());

            encoder.notifyMatchFound(p1.state, matchInfoForP1);
            encoder.notifyMatchFound(p2.state, matchInfoForP2);
        }
    }

    public synchronized int getQueueSize() {
        return queue.size();
    }
}

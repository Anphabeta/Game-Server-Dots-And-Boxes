package controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import dao.MatchDAO;
import dao.UserDAO;
import game.MoveResult;
import game.Room;
import game.RoomManager;
import model.LeaderboardEntry;
import model.User;
import network.ClientState;
import request.Encoder;

public class UserRepresentative {
    public final int sessionId;
    public final User user;
    public ClientState state;
    private final Consumer<Integer> deleteItself;
    private final HeartBeatManager heartBeatManager;
    
    private UserDAO userDAO;
    private MatchDAO matchDAO;
    private Encoder encoder;
    private MatchMakingManager matchMakingManager;
    private Room currentRoom;
    private RoomManager roomManager;
    
    private static final Map<String, BiConsumer<UserRepresentative, String>> individualMap = new HashMap<>();
    
    static {
        individualMap.put("GET_USER_DETAIL", UserRepresentative::getUserDetail);
        individualMap.put("JOIN_QUICK_MATCH", UserRepresentative::joinQuickMatch);
        individualMap.put("CANCEL_QUICK_MATCH", UserRepresentative::cancelQuickMatch);
        individualMap.put("MAKE_MOVE", UserRepresentative::makeMove);
        individualMap.put("SURRENDER", UserRepresentative::surrender);
        individualMap.put("GET_USERS_AVAILABLE_LIST", UserRepresentative::getUsersAvailableList);    
        individualMap.put("GET_LEADERBOARD", UserRepresentative::getLeaderboard); 
    }

    public UserRepresentative(int sessionId, User user, ClientState state, Consumer<Integer> deleteItself) {
        this.sessionId = sessionId;
        this.user = user;
        this.state = state;
        this.deleteItself = deleteItself;
        this.heartBeatManager = new HeartBeatManager(()->deleteRepresentative(), 10);
    }

    public void sendRestoreState() {
        System.out.println("=== RESTORE DEBUG ===");
        System.out.println("User: " + user.getUsername());
        System.out.println("Room: " + currentRoom);

        if (currentRoom != null) {
            System.out.println("Room finished: " + currentRoom.isFinished());
        }

        if (currentRoom != null && !currentRoom.isFinished()) {
            System.out.println("SENDING RESTORE_MATCH");
            currentRoom.sendRestoreSnapshot(this);
        } else {
            System.out.println("NO ACTIVE MATCH");
        }
    }

    public void reattachState(ClientState newState) {
        this.state = newState;
    }

    public void setDAO(UserDAO userDAO, MatchDAO matchDAO){
        this.userDAO = userDAO;
        this.matchDAO = matchDAO;
    }
    
    public void setEncoder(Encoder encoder){
        this.encoder = encoder;
    }

    public void setMatchMakingManager(MatchMakingManager matchMakingManager){
        this.matchMakingManager = matchMakingManager;
    }

    public void setRoomManager(RoomManager roomManager) {
        this.roomManager = roomManager;
    }

    public void setCurrentRoom(Room currentRoom) {
        this.currentRoom = currentRoom;
    }

    public Room getCurrentRoom() {
        return currentRoom;
    }
    
    private void deleteRepresentative() {
        // Dừng heartbeat của phiên đã hết hạn
        heartBeatManager.stop();

        // Dọn khỏi hàng đợi và xử lý trận đấu nếu đang chơi
        deleteDependency();

        // Đánh dấu người chơi ngoại tuyến trong bộ nhớ
        user.setStatus(User.STATUS.OFFLINE);

        // Đồng bộ trạng thái xuống MySQL
        if (userDAO != null) {
            boolean updated = userDAO.updateUserStatus(
                user.getUserId(),
                User.STATUS.OFFLINE
            );

            System.out.println(
                "[STATUS] Disconnect timeout: "
                + user.getUsername()
                + " -> offline, DB updated=" + updated
            );
        } else {
            System.err.println(
                "[STATUS] Cannot update offline status: UserDAO is null for "
                + user.getUsername()
            );
        }

        // Xóa đại diện của phiên đã hết hạn
        deleteItself.accept(sessionId);

        System.out.println(
            "[SESSION] Representative removed: " + sessionId
        );
    }
    
    private void deleteDependency(){
        if (matchMakingManager != null) {
            matchMakingManager.removePlayer(this);
            matchMakingManager.removeWaitingRoom(this);
        }        
        
        if (currentRoom != null && !currentRoom.isFinished()) {
            currentRoom.handleDisconnect(user.getUserId());
            UserRepresentative opponent = currentRoom.getOpponentRepByUserId(user.getUserId());
            if (encoder != null && opponent != null) {
                encoder.notifyOpponentDisconnect(opponent.state, user.getUserId());
            }
        }
    }
    
    public void resetSchedule(){
        heartBeatManager.resetSchedule();
    }
    
    public void shutdownSchedule(){
        heartBeatManager.stop();
    }
    
    
    // WRITE FLOW
    
    public void replyAttach(){
        String status = "ATTACH_SUCCESS";
        state.writeFrame(String.format("REPLY_ATTACH,,%s", status));
    }
    
    public void receiveChat(String message, int orgUserId){
        state.writeFrame(String.format("RECEIVE_CHAT,,%d;%s", orgUserId, message));
    }
    
    public void unableToChat(String status){
        state.writeFrame(String.format("UNABLE_TO_CHAT,,%s", status));
    }
    
    
    // READ FLOW
    
    public void readMessage(String opCode, String params){
        BiConsumer<UserRepresentative, String> handler = individualMap.get(opCode);
        if(handler!=null){
            handler.accept(this, params);            
        }
        else{
            System.out.println("Handler doesn't exist in Presentative");
        }
    }

    private void getUserDetail(String params) {
        String userDetail = String.format("%s;%s;%d;%s;%d",
                user.getUsername(),
                user.getDisplayName(),
                user.getRanking(),
                user.getStatus(),
                user.getUserId()
        );

        state.writeFrame(String.format("RETURN_USER_DETAIL,,%s", userDetail));
    }
    
    private void getLeaderboard(String params) {
        if (userDAO == null || encoder == null) {
            System.err.println(
                "[LEADERBOARD] UserDAO or Encoder is null"
            );
            return;
        }

        List<LeaderboardEntry> entries = userDAO.getLeaderboard(7);

        System.out.println(
            "[LEADERBOARD] Retrieved " + entries.size() + " users"
        );

        encoder.replyLeaderboard(state, entries);
    }

    public void responseInvitation(String status) {
        
        encoder.responseInvitation(state, status);
    }    
    
    /**
     * Hàm này có do người dùng gọi có tác dụng đưa người chơi vào trận đấu.<br>
     * Luồng hoạt động chính:<br>
     * - Chuyển trạng thái người dùng từ ONLINE thành QUEUING<br>
     * - Đưa người chơi vào hàng đợi trong {@link MatchMakingManager}<br>
     * Luồng ngoại lệ:<br>
     * - Nếu không tồn tại {@link MatchMakingManager} thì hủy hàm<br>
     * - Nếu trạng thái hiện tại không phải là ONLINE thì gủi phản hồi ALREADY_BUSY cho client và hủy hàm<br>
     * - Nếu người dùng spam click vào quick match, hàm này sẽ gọi nhiều lần -> thêm liên tục người dùng vào hàng đợi. Do đó cần kiểm tra, nếu người dùng đã trong hàng đợi thì không thêm nữa và gửi ALREADY_IN_QUEUE cho client<br>
     */
    private void joinQuickMatch(String params) {
        if (matchMakingManager == null) {
            System.out.println("MatchMakingManager is null for session " + sessionId);
            return;
        }

        // 1. Kiểm tra trạng thái hiện tại (chỉ cho phép khi đang ONLINE)
        if (!User.STATUS.ONLINE.equalsIgnoreCase(user.getStatus())) {
            if (encoder != null) {
                encoder.replyJoinQuickMatch(state, "ALREADY_BUSY");
            }
            return;
        }

        // 2. Chuyển trạng thái sang tìm trận
        user.setStatus(User.STATUS.QUEUING);

        // 3. Đưa vào MatchMakingManager với mode được chọn từ client (hoặc ANY nếu rỗng)
        String requestedMode = (params == null || params.trim().isEmpty()) ? MatchMakingManager.MODE_ANY : params.trim();
        boolean added = matchMakingManager.addPlayer(this, requestedMode);
        if (added) {
            if (encoder != null) {
                String assignedSize = matchMakingManager.getAssignedSize(this);
                encoder.replyJoinQuickMatch(state, "SUCCESS", assignedSize);
            }
        } else {
            user.setStatus(User.STATUS.ONLINE);
            if (encoder != null) {
                encoder.replyJoinQuickMatch(state, "ALREADY_IN_QUEUE");
            }
        }
    }

    /**
     * Hàm này có tác dụng loại bỏ người chơi khỏi hàng {@link MatchMakingManager}<br>
     * Luồng hoạt động chính:<br>
     * - Nếu người chơi đang ở trạng thái QUEUING (tức đang trong hàng đợi) thì xóa người chơi khỏi hàng đợi<br>
     * - Đặt lại trạng thái QUEUING -> ONLINE<br>
     * - Gửi phản hồi thành công cho client<br>
     * Luồng ngoại lệ:<br>
     * - Nếu không tồn tại {@link MatchMakingManager} thì hủy hàm<br>
     * - Nếu trạng thái người dùng không phải QUEUING -> phản hồi lại client NOT_IN_QUEUE -> hủy hàm<br>
     */
    private void cancelQuickMatch(String params) {
        if (matchMakingManager == null) {
            return;
        }

        if (User.STATUS.QUEUING.equalsIgnoreCase(user.getStatus())) {
            matchMakingManager.removePlayer(this);
            user.setStatus(User.STATUS.ONLINE);
            if (encoder != null) {
                encoder.replyCancelQuickMatch(state, "SUCCESS");
            }
        } else {
            if (encoder != null) {
                encoder.replyCancelQuickMatch(state, "NOT_IN_QUEUE");
            }
        }
    }

    private void makeMove(String params) {
        if (currentRoom == null && roomManager != null) {
            currentRoom = roomManager.getRoomByUserId(user.getUserId());
        }

        if (currentRoom == null || currentRoom.isFinished()) {
            if (encoder != null) {
                encoder.replyInvalidMove(state, "NOT_IN_MATCH");
            }
            return;
        }

        try {
            String[] parts = params.split(";");
            if (parts.length < 4) {
                if (encoder != null) {
                    encoder.replyInvalidMove(state, "INVALID_PARAMS");
                }
                return;
            }

            int r1 = Integer.parseInt(parts[0].trim());
            int c1 = Integer.parseInt(parts[1].trim());
            int r2 = Integer.parseInt(parts[2].trim());
            int c2 = Integer.parseInt(parts[3].trim());

            MoveResult result = currentRoom.handleMove(user.getUserId(), r1, c1, r2, c2);
            if (!result.isSuccess()) {
                if (encoder != null) {
                    encoder.replyInvalidMove(state, result.getMessage());
                }
                return;
            }

            // Gửi kết quả nước đi cho cả 2 người chơi trong phòng
            UserRepresentative rep1 = currentRoom.getPlayer1Rep();
            UserRepresentative rep2 = currentRoom.getPlayer2Rep();
            int p1Score = currentRoom.getPlayer1().getScore();
            int p2Score = currentRoom.getPlayer2().getScore();

            if (encoder != null) {
                if (rep1 != null) encoder.notifyMoveResult(rep1.state, p1Score, p2Score, result);
                if (rep2 != null) encoder.notifyMoveResult(rep2.state, p1Score, p2Score, result);
            }
        } catch (NumberFormatException e) {
            if (encoder != null) {
                encoder.replyInvalidMove(state, "INVALID_NUMBER_FORMAT");
            }
        }
    }

    private void surrender(String params) {
        if (currentRoom == null && roomManager != null) {
            currentRoom = roomManager.getRoomByUserId(user.getUserId());
        }

        if (currentRoom == null || currentRoom.isFinished()) {
            return;
        }

        currentRoom.handleResign(user.getUserId());
        UserRepresentative opponent = currentRoom.getOpponentRepByUserId(user.getUserId());
        if (encoder != null && opponent != null) {
            encoder.notifyPlayerSurrender(opponent.state, user.getUserId());
        }
    }
    
    private void getUsersAvailableList(String params) {
        if (userDAO == null || encoder == null) {
            System.err.println("[USERS] UserDAO or Encoder is null");
            return;
        }

        // Lấy tất cả tài khoản đã đăng ký từ MySQL
        List<User> users = userDAO.getAllUsers();

        System.out.println("[USERS] Retrieved " + users.size() + " users");

        // Gửi toàn bộ danh sách về frontend
        encoder.replyAllUsersList(state, users);
    }

    /**
     * Tạo lời mời tới Representative hiện tại, người gửi là sender
     * @param sender : Là representative của người gửi.<br>
     * 
     */
    void invite(UserRepresentative sender, String boardSize) {
        if (sender == null) {
            return;
        }

        if (User.STATUS.ONLINE.equalsIgnoreCase(user.getStatus())) {
            encoder.sendInvitation(
                state,
                sender.user.getUserId(),
                sender.user.getDisplayName(),
                boardSize
            );

            sender.replyInvite("SUCCESS");
        }
        else if (User.STATUS.INGAME.equalsIgnoreCase(user.getStatus())) {
            sender.replyInvite("USER_INGAME");
        }
        else {
            sender.replyInvite("USER_UNAVAILABLE");
        }
    }

    public void replyInvite(String replyState) {
        encoder.replyInvite(state, replyState);
    }


    
}

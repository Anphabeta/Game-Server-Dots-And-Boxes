package controller;

import java.nio.channels.SelectionKey;

import dao.MatchDAO;
import dao.UserDAO;
import game.RoomManager;
import model.User;
import network.ClientState;
import request.Encoder;

public class MasterController {
    private static int maxSessionId = 1;
    public final RepresentativeManager representativeManager = new RepresentativeManager();
    public final MatchMakingManager matchMakingManager = new MatchMakingManager();
    public final RoomManager roomManager = RoomManager.getInstance();
    private Encoder encoder;
    
    private final UserDAO userDAO = new UserDAO();
    private final MatchDAO matchDAO = new MatchDAO();


    public void setEncoder(Encoder encoder) {
        this.encoder = encoder;
        this.matchMakingManager.setEncoder(encoder);
        this.matchMakingManager.setRoomManager(roomManager);
        this.matchMakingManager.setDAO(userDAO, matchDAO);
    }

    public void resetStaleUserStatuses() {
        int count = userDAO.resetStaleUserStatuses();

        if (count >= 0) {
            System.out.println(
                "[STATUS] Startup status reset completed"
            );
        } else {
            System.err.println(
                "[STATUS] Startup status reset failed"
            );
        }
    }

    // General function
    
    private void addRepresentative(User user, int sessionId, ClientState state){
        UserRepresentative representative = new UserRepresentative(sessionId, user, state, (id)->removeRepresentative(id));
        representative.setDAO(userDAO, matchDAO);
        representative.setEncoder(encoder);
        representative.setMatchMakingManager(matchMakingManager);
        representative.setRoomManager(roomManager);
        representativeManager.put(sessionId, representative);
    }
    
    private void removeRepresentative(int sessionId){
        UserRepresentative repst = representativeManager.getBySessionId(sessionId);
        if (repst != null) {
            matchMakingManager.removePlayer(repst);
        }
        representativeManager.removeBySessionId(sessionId);
        System.out.println(representativeManager.size());
    }

    public void ping(SelectionKey key, int sessionId) {
//        System.out.println("Da toi Controller");
        if(representativeManager.containsSessionIdKey(sessionId)){
            representativeManager.getBySessionId(sessionId).resetSchedule();
        }
        encoder.pong(key);
    }

    public void register(SelectionKey key, String userName, String password, String displayName){
        String registerStatus;
        if(userDAO.isUsernameExists(userName)){
            registerStatus = "USERNAME_EXISTS";
            encoder.replyRegister(key, registerStatus);
            return;
        }
        boolean status = userDAO.createUser(new User(userName, password, displayName));
        registerStatus = status ? "REGISTER_SUCCESS" : "REGISTER_FAIL";
        encoder.replyRegister(key, registerStatus);
    }
    
    public void login(SelectionKey key, String userName, String password){
        String loginStatus;        
        if(!userDAO.isUsernameExists(userName)){
            loginStatus = "UNKNOWN_USERNAME";
            encoder.replyLogin(key, loginStatus);
            return;
        }
        if(!userDAO.isValidPassword(userName, password)){
            loginStatus = "WRONG_PASSWORD";
            encoder.replyLogin(key, loginStatus);
            return;
        }
        
        User user = userDAO.findByUsernameAndPassword(userName, password);
        UserRepresentative existing = representativeManager.getByUserId(user.getUserId());

        if (existing != null) {
            if (existing.state != null && existing.state.isValid()) {
                // Vẫn còn 1 tab/kết nối khác đang sống thật sự
                encoder.replyLogin(key, "ACCOUNT_ALREADY_ONLINE");
                return;
            }

            // Representative còn tồn tại nhưng ClientState cũ đã chết (đóng tab / rớt mạng)
            // -> KHÔI PHỤC vào kết nối mới này, giữ nguyên sessionId cũ
            ClientState newState = (ClientState) key.attachment();
            existing.reattachState(newState);
            newState.setSessionId(existing.sessionId);
            existing.resetSchedule();           // reset đồng hồ heartbeat
            
            // Đồng bộ trạng thái hiện tại của phiên người dùng vào MySQL.
            // Giữ nguyên trạng thái hiện tại để không vô tình chuyển
            // người đang chơi thành người đang rảnh.
            String currentStatus = existing.user.getStatus();

            if (currentStatus == null || currentStatus.trim().isEmpty()) {
                currentStatus = User.STATUS.ONLINE;
                existing.user.setStatus(currentStatus);
            }

            boolean statusUpdated = userDAO.updateUserStatus(
                existing.user.getUserId(),
                currentStatus
            );

            System.out.println(
                "[STATUS] Reconnect " + existing.user.getUsername()
                + " -> " + currentStatus
                + ", DB updated=" + statusUpdated
            );

            encoder.replyLogin(key, "LOGIN_SUCCESS");
            encoder.setClientSessionId(key, existing.sessionId);
            existing.sendRestoreState();        // xem mục 2.4
            return;
        }
        else{
            loginStatus = "LOGIN_SUCCESS";
            encoder.replyLogin(key, loginStatus);
            encoder.setClientSessionId(key, maxSessionId);
            
            user.setStatus(User.STATUS.ONLINE);

            // Cập nhật trạng thái online vào MySQL
            boolean statusUpdated = userDAO.updateUserStatus(
                user.getUserId(),
                User.STATUS.ONLINE
            );

            System.out.println(
                "[STATUS] Login " + user.getUsername()
                + " -> online, DB updated=" + statusUpdated
            );

            ClientState state = (ClientState) key.attachment();
            addRepresentative(user, maxSessionId, state);

            state.setSessionId(maxSessionId);
            maxSessionId++;
            
            System.out.println("User "+user.getUsername()+" logged in");
        }
    }
    
    public void acceptSessionId(SelectionKey key, int sessionId){
        ClientState state = (ClientState) key.attachment();
        state.setSessionId(sessionId);
    }
    
    public void logout(SelectionKey key, int sessionId){
        UserRepresentative repst = representativeManager.getBySessionId(sessionId);
        if (repst != null) {
            repst.shutdownSchedule();
            matchMakingManager.removePlayer(repst);
            if (repst.getCurrentRoom() != null && !repst.getCurrentRoom().isFinished()) {
                repst.getCurrentRoom().handleDisconnect(repst.user.getUserId());
            }
        }
        userDAO.updateUserStatus(repst != null ? repst.user.getUserId() : -1, User.STATUS.OFFLINE);
        representativeManager.removeBySessionId(sessionId);
        System.out.println("Removed representative");
        encoder.replyLogout(key);
        ((ClientState) key.attachment()).setSessionId(-1);
        System.out.println("Set sessionId -1");
    }
    
    public void attachRequest(SelectionKey key, int sessionId){
        if(representativeManager.containsSessionIdKey(sessionId)){
            UserRepresentative representative = representativeManager.getBySessionId(sessionId);
            encoder.replaceClientState((ClientState) key.attachment(), representative.state);
            key.attach(representative.state);
            representative.state.setKey(key); 
            
            representative.replyAttach();
            representative.resetSchedule();
            representative.sendRestoreState();
        }
        else{
            encoder.replyLogout(key);
        }
    }
    
    public void chatIndividual(int srcSessionId, int desUserId, String message){
        UserRepresentative srcRepst = representativeManager.getBySessionId(srcSessionId);
        if(representativeManager.containsUserIdKey(desUserId)){
            UserRepresentative desRepst = representativeManager.getByUserId(desUserId);
            int srcUserId = srcRepst.user.getUserId();
            desRepst.receiveChat(message, srcUserId);
        }
        else{
            srcRepst.unableToChat("USER_OFFLINE");
        }
    }

    public void invite(SelectionKey key, int orgUserId, int desUserId, String broadSize) {
        UserRepresentative sender = representativeManager.getByUserId(orgUserId);
        if(representativeManager.containsUserIdKey(desUserId)){
            representativeManager.getByUserId(desUserId).invite(sender, broadSize);
        }
        else{
            sender.replyInvite("USER_UNAVAILABLE");
        }
    }

    public void reponseInvite(SelectionKey key, int orgUserId, int desUserId, String broadSize, String status) {
        if(representativeManager.containsUserIdKey(desUserId)){
            representativeManager.getByUserId(desUserId).responseInvitation(status);            
        }
        
        if("ACCEPT".equalsIgnoreCase(status)){
            UserRepresentative owner = representativeManager.getByUserId(desUserId);
            UserRepresentative other = representativeManager.getByUserId(orgUserId);
            matchMakingManager.matchInvitation(owner, other, broadSize);
        }
    }    
    
    public void shareRoom(SelectionKey key, int ownerUserId, String boardSize){
        UserRepresentative owner = representativeManager.getByUserId(ownerUserId);
        if (owner == null) {
            encoder.replyShareRoom(key, "CREATE_FAILED");
            return;
        }

        // Kiểm tra trạng thái hiện tại (chỉ cho phép tạo phòng khi ONLINE)
        if (User.STATUS.QUEUING.equalsIgnoreCase(owner.user.getStatus())) {
            encoder.replyShareRoom(key, "ALREADY_IN_QUEUE");
            return;
        }
        if (!User.STATUS.ONLINE.equalsIgnoreCase(owner.user.getStatus())) {
            encoder.replyShareRoom(key, "ALREADY_BUSY");
            return;
        }

        String roomCode = matchMakingManager.createWaitingRoom(owner, boardSize);

        if(roomCode != null){
            owner.user.setStatus(User.STATUS.WAITING_ROOM);
            encoder.replyShareRoom(key, "SUCCESS", roomCode);
        }
        else{
            System.out.println("Create waiting room failure");
            encoder.replyShareRoom(key, "CREATE_FAILED");
        }        
    }

    public void cancelShareRoom(SelectionKey key, int ownerUserId){
        UserRepresentative owner = representativeManager.getByUserId(ownerUserId);
        if (owner == null) {
            encoder.replyCancelShareRoom(key, "FAIL");
            return;
        }

        boolean removed = matchMakingManager.removeWaitingRoom(owner);
        if (removed || User.STATUS.WAITING_ROOM.equalsIgnoreCase(owner.user.getStatus())) {
            owner.user.setStatus(User.STATUS.ONLINE);
            encoder.replyCancelShareRoom(key, "SUCCESS");
        } else {
            encoder.replyCancelShareRoom(key, "NOT_IN_WAITING_ROOM");
        }
    }
    
    public void findRoom(SelectionKey key, int orgUserId, String roomCode){
        UserRepresentative user = representativeManager.getByUserId(orgUserId);
        if (user == null) {
            return;
        }

        // Kiểm tra trạng thái người tham gia
        if (!User.STATUS.ONLINE.equalsIgnoreCase(user.user.getStatus())) {
            encoder.replyFindRoom(user.state, "ALREADY_BUSY");
            return;
        }

        matchMakingManager.findWaitingRoom(roomCode, user);
    }
    
    
    // Dispatch individual
    
    public void dispatchRepresentative(int sessionId, String opCode, String params, SelectionKey key){
        if(representativeManager.containsSessionIdKey(sessionId)){
            representativeManager.getBySessionId(sessionId).readMessage(opCode, params);        
        }
        else{
            encoder.replyLogout(key);
        }
    }

}

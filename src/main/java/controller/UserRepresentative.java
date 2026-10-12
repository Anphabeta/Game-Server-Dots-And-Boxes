package controller;

import dao.MatchDAO;
import dao.UserDAO;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import model.User;
import network.ClientState;
import request.Encoder;

public class UserRepresentative {
    public final int sessionId;
    public final User user;
    public final ClientState state;
    private final Consumer<Integer> deleteItself;
    private final HeartBeatManager heartBeatManager;
    
    private UserDAO userDAO;
    private MatchDAO matchDAO;
    private Encoder encoder;
    private MatchMakingManager matchMakingManager;
    
    private static final Map<String, BiConsumer<UserRepresentative, String>> individualMap = new HashMap<>();
    
    static {
        individualMap.put("GET_USER_DETAIL", UserRepresentative::getUserDetail);
        individualMap.put("JOIN_QUICK_MATCH", UserRepresentative::joinQuickMatch);
        individualMap.put("CANCEL_QUICK_MATCH", UserRepresentative::cancelQuickMatch);
    }

    public UserRepresentative(int sessionId, User user, ClientState state, Consumer<Integer> deleteItself) {
        this.sessionId = sessionId;
        this.user = user;
        this.state = state;
        this.deleteItself = deleteItself;
        this.heartBeatManager = new HeartBeatManager(()->deleteRepresentative(), 10000);
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
    
    private void deleteRepresentative(){
        heartBeatManager.stop();
        if (matchMakingManager != null) {
            matchMakingManager.removePlayer(this);
        }
        deleteItself.accept(sessionId);
        System.out.println("Representative delete itself");
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
        state.writeFrame(String.format("REPLY_ATTACH,,", status));
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
        String userDetail = String.format("%s;%s;%d;%s", 
                user.getUsername(), 
                user.getDisplayName(),
                user.getRanking(),
                user.getStatus()
        );
        state.writeFrame(String.format("RETURN_USER_DETAIL,,%s", userDetail));
    }
    
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

        // 3. Đưa vào MatchMakingManager
        boolean added = matchMakingManager.addPlayer(this);
        if (added) {
            if (encoder != null) {
                encoder.replyJoinQuickMatch(state, "SUCCESS");
            }
        } else {
            user.setStatus(User.STATUS.ONLINE);
            if (encoder != null) {
                encoder.replyJoinQuickMatch(state, "ALREADY_IN_QUEUE");
            }
        }
    }

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
}

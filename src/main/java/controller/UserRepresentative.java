package controller;

import dao.MatchDAO;
import dao.UserDAO;
import java.util.HashMap;
import java.util.Map;
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
    
    private static final Map<String, Consumer<String>> individualMap = new HashMap<>();
    
    private void initIndividualFunctionMap(){
        individualMap.put("GET_USER_DETAIL", (params)->getUserDetail(params));
    }

    public UserRepresentative(int sessionId, User user, ClientState state, Consumer<Integer> deleteItself) {
        initIndividualFunctionMap();
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
    
    private void deleteRepresentative(){
        heartBeatManager.stop();
        deleteItself.accept(sessionId);
        System.out.println("Representative delete itself");
    }
    
    public void resetSchedule(){
        heartBeatManager.resetSchedule();
    }
    
    public void shutdownSchedule(){
        heartBeatManager.stop();
    }
    
    public void replyAttach(){
        encoder.replyAttach(state, "ATTACH_SUCCESS");
    }
    
    public void readMessage(String opCode, String params){
        Consumer<String> handler = individualMap.get(opCode);
        if(handler!=null){
            handler.accept(params);            
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
        encoder.returnUserDetail(state, userDetail);
    }
}

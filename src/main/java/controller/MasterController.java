package controller;

import dao.MatchDAO;
import dao.UserDAO;
import java.nio.channels.SelectionKey;
import model.User;
import network.ClientState;
import request.Encoder;

public class MasterController {
    private static int maxSessionId = 1;
    public final RepresentativeManager representativeManager = new RepresentativeManager();
    public final MatchMakingManager matchMakingManager = new MatchMakingManager();
    private Encoder encoder;
    
    private final UserDAO userDAO = new UserDAO();
    private final MatchDAO matchDAO = new MatchDAO();


    public void setEncoder(Encoder encoder) {
        this.encoder = encoder;
        this.matchMakingManager.setEncoder(encoder);
    }

    // General function
    
    private void addRepresentative(User user, int sessionId, ClientState state){
        UserRepresentative representative = new UserRepresentative(sessionId, user, state, (id)->removeRepresentative(id));
        representative.setDAO(userDAO, matchDAO);
        representative.setEncoder(encoder);
        representative.setMatchMakingManager(matchMakingManager);
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
        if(representativeManager.containsUserIdKey(user.getUserId())){
            loginStatus = "ACCOUNT_ALREADY_ONLINE";
            encoder.replyLogin(key, loginStatus);           
        }
        else{
            loginStatus = "LOGIN_SUCCESS";
            encoder.replyLogin(key, loginStatus);
            encoder.setClientSessionId(key, maxSessionId);
            
            user.setStatus(User.STATUS.ONLINE);
            ClientState state = (ClientState) key.attachment();
            addRepresentative(user, maxSessionId, state);         
            state.setSessionId(maxSessionId);

            maxSessionId++;             
        }
    }
    
    public void acceptSessionId(SelectionKey key, int sessionId){
        ClientState state = (ClientState) key.attachment();
        state.setSessionId(sessionId);
    }
    
    public void logout(SelectionKey key, int sessionId){
        UserRepresentative repst = representativeManager.getBySessionId(sessionId);
        if (repst != null) {
            matchMakingManager.removePlayer(repst);
        }
        representativeManager.removeBySessionId(sessionId);
        System.out.println("Removed representative");
        encoder.replyLogout(key);
        ((ClientState) key.attachment()).setSessionId(-1);
        System.out.println("Set sessionId -1");
    }
    
    public void chatBroadcast(String message){
        encoder.chatBroadcast(message);
    }
    
    public void attachRequest(SelectionKey key, int sessionId){
        if(representativeManager.containsSessionIdKey(sessionId)){
            UserRepresentative representative = representativeManager.getBySessionId(sessionId);
            encoder.replaceClientState((ClientState) key.attachment(), representative.state);
            key.attach(representative.state);
            representative.state.setKey(key); 
            
            representative.replyAttach();
            representative.resetSchedule();
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

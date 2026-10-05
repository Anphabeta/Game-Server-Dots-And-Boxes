package controller;

import dao.MatchDAO;
import dao.UserDAO;
import java.nio.channels.SelectionKey;
import java.util.LinkedHashMap;
import java.util.Map;
import model.User;
import request.Encoder;

public class MasterController {
    private static int maxSessionId = 0;
    public final Map<Integer, UserRepresentative> representatives = new LinkedHashMap<>();
    private Encoder encoder;
    
    private UserDAO userDAO;
    private MatchDAO matchDAO;


    public void setEncoder(Encoder encoder) {
        this.encoder = encoder;
    }

    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public void setMatchDAO(MatchDAO matchDAO) {
        this.matchDAO = matchDAO;
    }
    
    
    
    private void addRepresentative(User user){
        maxSessionId++;
        UserRepresentative representative = new UserRepresentative(maxSessionId, user);
        representatives.put(maxSessionId, representative);
    }
    
    public void authenticateAccount(SelectionKey key, String userName, String hashedPassword){
        if(userDAO.isUsernameExists(userName)){
            if(userDAO.isValidPassword(userName, hashedPassword)){
                User user = userDAO.findByUsername(userName);
                addRepresentative(user);                
            }
            else{
                encoder.wrongPassword(key);
            }
        }
        else{
            encoder.accountNotExist(key);
        }
    }

    public void ping(SelectionKey key) {
//        System.out.println("Da toi Controller");
        encoder.pong(key);
    }
}

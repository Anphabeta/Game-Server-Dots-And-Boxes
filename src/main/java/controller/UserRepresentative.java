package controller;

import model.User;

public class UserRepresentative {
    public final int sessionId;
    public final User user;

    public UserRepresentative(int sessionId, User user) {
        this.sessionId = sessionId;
        this.user = user;
    }
    
    
}

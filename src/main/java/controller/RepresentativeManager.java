package controller;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class RepresentativeManager{
    private final Map<Integer, UserRepresentative> repstBySessionId = new ConcurrentHashMap<>();
    private final Map<Integer, UserRepresentative> repstByUserId = new ConcurrentHashMap<>();

    public void put(int sessionId, UserRepresentative representative){
        repstBySessionId.put(sessionId, representative);
        repstByUserId.put(representative.user.getUserId(), representative);
    }

    public void removeBySessionId(int sessionId){
        UserRepresentative repst = repstBySessionId.remove(sessionId);
        if (repst != null) {
            repstByUserId.remove(repst.user.getUserId());
            repst.shutdownSchedule();
        }
    }
    
    public void removeByUserId(int userId){
        UserRepresentative repst = repstByUserId.remove(userId);
        if(repst != null){
            repstBySessionId.remove(repst.sessionId);
            repst.shutdownSchedule();
        }
    }

    public int size() {
        return repstBySessionId.size();
    }

    public boolean containsSessionIdKey(int sessionId) {
        return repstBySessionId.containsKey(sessionId);
    }
    
    public boolean containsUserIdKey(int userId){
        return repstByUserId.containsKey(userId);
    }

    public UserRepresentative getBySessionId(int sessionId) {
        return repstBySessionId.get(sessionId);
    }
    
    public UserRepresentative getByUserId(int userId){
        return repstByUserId.get(userId);
    }
}

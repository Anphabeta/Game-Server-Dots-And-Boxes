package request;

import java.nio.channels.SelectionKey;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import game.Box;
import game.MoveResult;
import java.util.List;
import model.User;
import network.ClientState;
import network.NioServer;

public class Encoder {
    private NioServer network;

    public void setNetwork(NioServer network) {
        this.network = network;
    }
    
    private static ClientState getState(SelectionKey key){
        return (ClientState) key.attachment();
    }

    public void replyRegister(SelectionKey key, String registerStatus){
        getState(key).writeFrame(String.format("REPLY_REGISTER,,%s", registerStatus));
    }
    
    public void replyLogin(SelectionKey key, String loginStatus){
        getState(key).writeFrame(String.format("REPLY_LOGIN,,%s", loginStatus));
    }
    
    public void replyLogout(SelectionKey key){
        getState(key).writeFrame("REPLY_LOGOUT,,");
    }
    
    public void setClientSessionId(SelectionKey key, int maxSessionId) {
        getState(key).writeFrame(String.format("SET_SESSION_ID,,%s", String.valueOf(maxSessionId)));
    }

    public void pong(SelectionKey key) {
        getState(key).writeFrame("PONG,,");
    }
    
    public void chatBroadcast(String message){
//        System.out.println(network.clientStateList.size());
        for(ClientState state: network.clientStateList){
            state.writeFrame(String.format("CHAT_BROADCAST,,%s", message));
        }
    }
    
    public void replaceClientState(ClientState oldState, ClientState newState){
        network.clientStateList.remove(oldState);
        network.clientStateList.add(newState);
        System.out.println("Replaced temp client state before attach new one");
    }

    public void returnUserDetail(ClientState state, String userDetail) {
        state.writeFrame(String.format("RETURN_USER_DETAIL,,%s", userDetail));
    }

    public void replyJoinQuickMatch(ClientState state, String status) {
        state.writeFrame(String.format("REPLY_JOIN_QUICK_MATCH,,%s", status));
    }

    public void replyJoinQuickMatch(ClientState state, String status, String boardSize) {
        if (boardSize != null && !boardSize.isEmpty()) {
            state.writeFrame(String.format("REPLY_JOIN_QUICK_MATCH,,%s;%s", status, boardSize));
        } else {
            replyJoinQuickMatch(state, status);
        }
    }

    public void replyCancelQuickMatch(ClientState state, String status) {
        state.writeFrame(String.format("REPLY_CANCEL_QUICK_MATCH,,%s", status));
    }

    public void notifyMatchFound(ClientState state, String matchData) {
        state.writeFrame(String.format("MATCH_FOUND,,%s", matchData));
    }

    public void replyInvalidMove(ClientState state, String reason) {
        if (state != null && state.isValid()) {
            state.writeFrame(String.format("INVALID_MOVE,,%s", reason));
        }
    }

    public void notifyMoveResult(ClientState state, int p1Score, int p2Score, MoveResult result) {
        if (state == null || !state.isValid() || result == null) return;

        StringBuilder boxesStr = new StringBuilder();
        for (int i = 0; i < result.getCompletedBoxes().size(); i++) {
            Box b = result.getCompletedBoxes().get(i);
            boxesStr.append(b.getRow()).append(":").append(b.getCol());
            if (i < result.getCompletedBoxes().size() - 1) {
                boxesStr.append("|");
            }
        }
        String boxes = boxesStr.length() > 0 ? boxesStr.toString() : "NONE";

        int nextTurnId = result.getNextTurn() != null ? result.getNextTurn().getId() : -1;
        int winnerId = result.getWinner() != null ? result.getWinner().getId() : (result.isDraw() ? 0 : -1);

        // Format: playedBy;r1;c1;r2;c2;completedBoxes;p1Score;p2Score;nextTurn;bonusTurn;gameOver;winnerId;isDraw
        String data = String.format("%d;%d;%d;%d;%d;%s;%d;%d;%d;%b;%b;%d;%b",
                result.getPlayedBy().getId(),
                result.getEdge().getDot1().getRow(),
                result.getEdge().getDot1().getCol(),
                result.getEdge().getDot2().getRow(),
                result.getEdge().getDot2().getCol(),
                boxes,
                p1Score,
                p2Score,
                nextTurnId,
                result.hasBonusTurn(),
                result.isGameOver(),
                winnerId,
                result.isDraw()
        );

        state.writeFrame(String.format("MOVE_RESULT,,%s", data));
    }

    public void notifyGameOver(ClientState state, int winnerId, int p1Score, int p2Score, boolean isDraw, String reason) {
        if (state != null && state.isValid()) {
            state.writeFrame(String.format("GAME_OVER,,%d;%d;%d;%b;%s", winnerId, p1Score, p2Score, isDraw, reason));
        }
    }

    public void notifyPlayerSurrender(ClientState state, int surrenderedUserId) {
        if (state != null && state.isValid()) {
            state.writeFrame(String.format("OPPONENT_SURRENDER,,%d", surrenderedUserId));
        }
    }

    public void notifyOpponentDisconnect(ClientState state, int disconnectedUserId) {
        if (state != null && state.isValid()) {
            state.writeFrame(String.format("OPPONENT_DISCONNECT,,%d", disconnectedUserId));
        }
    }

    public void replyUsersAvailableList(ClientState state, List<User> onlineUserList, List<User> ingameUserList) {
        StringBuilder sb = new StringBuilder("REPLY_USERS_AVAILABLE_LIST,,");
        
        for(User user: onlineUserList){
            sb.append(String.format("%d:%s:%s:%s|", 
                    user.getUserId(),
                    user.getUsername(),
                    user.getDisplayName(),
                    user.getStatus()
            ));
        }
        sb.setLength(sb.length()-1);
        sb.append(";");
        
        for(User user: ingameUserList){
            sb.append(String.format("%d:%s:%s:%s|", 
                    user.getUserId(),
                    user.getUsername(),
                    user.getDisplayName(),
                    user.getStatus()
            ));
        }
        sb.setLength(sb.length()-1);
        
        System.out.println(sb.toString());
        state.writeFrame(sb.toString());
    }

    /**
     * @param replyState : 
     * SUCCESS; USER_UNAVAILABLE; USER_INGAME
     */
    public void replyInvite(ClientState state, String replyState) {
        state.writeFrame(String.format("INVITED,,%s", replyState));
    }

    public void sendInvitation(ClientState state, int userId, String displayName, String boardSize) {
        state.writeFrame(String.format("INVITED_BY,,%d;%s;%s", userId, displayName, boardSize));
    }
    
    /**
     * @param status :
     * SUCCESS; FAIL
     */
    public void replyResponseInvite(ClientState state, String status){
        state.writeFrame(String.format("REPLY_RESPONSE_INVITE,,%s", status));
    }

    /**
     * Đưa phản hồi trạng thái lời mời của server cho người đã đưa ra lời mời
     * 
     * @param status: <br>
     * ACCEPT: Tìm được người chơi và ghép trận thành công, <br>
     * REJECTED: Tìm được người chơi nhưng họ từ chối, <br>
     * FAIL: Không tìm được người chơi<br>
     */
    public void responseInvitation(ClientState state, String status) {
        state.writeFrame(String.format("RESPONSE_INVITATION,,%s", status));
    }
    
    /**
     * @param status : SUCCESS, ALREADY_BUSY, ALREADY_IN_QUEUE, CREATE_FAILED
     */
    public void replyShareRoom(SelectionKey key, String status, String roomCode){
        if (roomCode != null && !roomCode.isEmpty()) {
            getState(key).writeFrame(String.format("REPLY_SHARE_ROOM,,%s;%s", status, roomCode));
        } else {
            getState(key).writeFrame(String.format("REPLY_SHARE_ROOM,,%s", status));
        }
    }

    public void replyShareRoom(SelectionKey key, String status){
        replyShareRoom(key, status, null);
    }

    public void replyCancelShareRoom(SelectionKey key, String status){
        getState(key).writeFrame(String.format("REPLY_CANCEL_SHARE_ROOM,,%s", status));
    }
    
    /**
     * @param status : SUCCESS, INVALID_CODE, FAIL, IS_FULL, CANNOT_JOIN_OWN_ROOM, ALREADY_BUSY, OWNER_BUSY
     */
    public void replyFindRoom(ClientState state, String status){
        state.writeFrame(String.format("REPLY_FIND_ROOM,,%s", status));
    }
}

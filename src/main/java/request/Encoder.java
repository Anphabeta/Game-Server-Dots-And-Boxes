package request;

import java.nio.channels.SelectionKey;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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
        System.out.println(network.clientStateList.size());
        for(ClientState state: network.clientStateList){
            state.writeFrame(String.format("CHAT_BROADCAST,,%s", message));
        }
    }

    public void returnUserDetail(ClientState state, String userDetail){
        state.writeFrame(String.format("RETURN_USER_DETAIL,,%s", userDetail));
    }
    
    
    public void replaceClientState(ClientState oldState, ClientState newState){
        network.clientStateList.remove(oldState);
        network.clientStateList.add(newState);
        System.out.println("Replaced temp client state before attach new one");
    }
    
    public void replyAttach(ClientState state, String status){
        state.writeFrame(String.format("REPLY_ATTACH,,%s", status));
    }
}

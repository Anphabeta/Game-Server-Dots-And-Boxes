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

    public void wrongPassword(SelectionKey key) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public void accountNotExist(SelectionKey key) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public void pong(SelectionKey key) {
        ClientState state = (ClientState) key.attachment();
        
        state.writeFrame("PONG,,");
    }
    
    public void chatBroadcast(String message){
        for(ClientState state: network.clientStateList){
            state.writeFrame(String.format("CHAT_BROADCAST,,%s", message));
        }
    }
}

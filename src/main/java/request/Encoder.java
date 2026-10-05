package request;

import java.nio.channels.SelectionKey;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import network.ClientState;
import network.NioServer;

public class Encoder {

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
}

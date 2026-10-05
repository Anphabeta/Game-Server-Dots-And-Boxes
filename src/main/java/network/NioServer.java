package network;

import request.Decoder;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Vấn đề đầu tiên của NIO Server là connection management. 
 * Cần làm 2 việc: Đăng ký selector OP_ACCEPT, Server accept. 
 * Nó có các nhiệm vụ:
 */
public class NioServer {

    private Thread networkThread;
    private static boolean connected;
    
    private HeartBeatManager heartBeatManager;
    
    
    private static int sessionId = -1;
    
    // ---------------------------------------
    private final int port;    
    
    private static ServerSocketChannel server;
    private static Selector selector;
    private Decoder decoder;
    // ---------------------------------------
    
    
    public NioServer(int port) {
        this.port = port;
        server = createServerSocket(port);
        selector = createSelector();
    }
    
    public void setDecoder(Decoder decoder){
        this.decoder = decoder;
    }
    
    private static ServerSocketChannel createServerSocket(int port){
        try {
            ServerSocketChannel newServer = ServerSocketChannel.open();
            newServer.configureBlocking(false);
            
            newServer.bind(new InetSocketAddress(port));
            System.out.println("Server created");
            return newServer;
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }
    
    private static Selector createSelector(){
        try {
            Selector newSelector = Selector.open();

            server.register(newSelector, SelectionKey.OP_ACCEPT);
            System.out.println("Selector created");
            return newSelector;
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }
    
    public void listenSocket() throws IOException{
        while(true){
            selector.select();
            
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            
            while(iterator.hasNext()){
                SelectionKey key = iterator.next();
                iterator.remove();
                if(key.isAcceptable()){
                    SocketChannel client = server.accept();
                    System.out.println("Connected to Client");
                    client.configureBlocking(false);
                    SelectionKey newKey = client.register(selector, SelectionKey.OP_READ);
                    
                    newKey.attach(new ClientState(newKey, decoder));
                }
                
                if(key.isReadable()){
                    ClientState state = (ClientState) key.attachment();
                    
                    readClient(key, state);
                }
                
                if(!key.isValid()){
                    continue;
                }
                
                if(key.isWritable()){
                    ClientState state = (ClientState) key.attachment();
                    
                    writeClient(key, state);
                }
            }
        }
    }
    
    private void readClient(SelectionKey key, ClientState state){
        SocketChannel client = (SocketChannel)key.channel();
        try {
            if(state.receivedFrame==null){
                if(state.receivedLengthBuffer.hasRemaining()){
                    int n = client.read(state.receivedLengthBuffer);

                    if(n==-1){
                        client.close();
                        key.cancel();
                        return;
                    }
                }

                if(state.receivedLengthBuffer.hasRemaining()){
                    return;
                }

                state.receivedLengthBuffer.flip();
                int payloadLength = state.receivedLengthBuffer.getInt();
                state.receivedLengthBuffer.clear();

                state.setFrame(payloadLength);
            }

            if(state.receivedFrame!=null && state.receivedFrame.hasRemaining()){
                int n = client.read(state.receivedFrame);

                if(n==-1){
                    client.close();
                    key.cancel();
                    return;                
                }
            }

            if(state.receivedFrame.hasRemaining()){
                return;
            }

            state.receivedFrame.flip();
            state.mergeFrame(state.receivedFrame);
            state.receivedFrame = null;            
        } catch (IOException e) {
            System.out.println("mot client da ngat ket noi");
            key.cancel();
            try {
                client.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

    }
    
    private void writeClient(SelectionKey key, ClientState state){
        SocketChannel client = (SocketChannel) key.channel();
        
        try {
            while(!state.writeQueue.isEmpty()){
                ByteBuffer currentBuffer = state.writeQueue.peek();
                
                client.write(currentBuffer);
                
                if(currentBuffer.hasRemaining()){
                    return;
                }
                
                state.writeQueue.poll();
            }
            
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);        
        } catch (Exception e) {
            e.printStackTrace();
            key.cancel();
            try {
                client.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

    }
}


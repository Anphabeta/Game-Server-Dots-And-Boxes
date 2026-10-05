package network;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import request.Decoder;

public class ClientState{
    public Message message = new Message();
    private final Decoder decoder;
    private final SelectionKey key;
    
    public ByteBuffer receivedLengthBuffer = ByteBuffer.allocate(4);
    public ByteBuffer receivedFrame;

    public Queue<ByteBuffer> writeQueue = new LinkedList<>();

    public ClientState(SelectionKey key, Decoder decoder) {
        this.key = key;
        this.decoder = decoder;
    }
    
    public void setFrame(int length){
        receivedFrame = ByteBuffer.allocate(12+length);
    }
    
    private static int getNumPartMessage(ByteBuffer frame){
        return frame.getShort(8);
    }
    
    public void mergeFrame(ByteBuffer frame){
        if(message.isEmptyFrameList()){
            message.countDown = getNumPartMessage(frame)-1;
            message.addFrame(frame);
        }
        else{
            message.addFrame(frame);
            message.countDown--;            
        }
        if(message.countDown==0){
            String rawMessage = message.unwrapMessage();
            message.countDown = -1;
            message.clearFrameList();
            
//            System.out.println(rawMessage);
            decoder.readMessage(key,rawMessage);
        }
    }
    
    public void writeFrame(String rawMessage){
//        System.out.println("Da toi Client State");
        ByteBuffer[] writeBuffers = message.wrapMessage(rawMessage);
        writeQueue.addAll(Arrays.asList(writeBuffers));
        
        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
    }
}
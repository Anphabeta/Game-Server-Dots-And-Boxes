package network;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 *
 * @author ACER
 */
public class Message {
    private static int messageId = -1;
    private String rawMessage;
    private int sessionId = -1;
    private final short MAX_PAYLOAD = 128;
    private final ArrayList<Frame> frameList = new ArrayList<>();
    
    public int countDown = -1;
    
    public void setSessionId(int id){
        sessionId = id;
    }
    
    public static void increaseMessageId(){
        messageId--;
    }
    
    public void clearFrameList(){
        frameList.clear();
    }
    
    public void addFrame(ByteBuffer frame){
        frameList.add(new Frame(frame));
    }
    
    public boolean isEmptyFrameList(){
        return frameList.isEmpty();
    }
    
    /**
     * Đóng gói dữ liệu String thô thành frame.<br>
     * Giao thức frame [byte length][session id][message id][number of part][part id][payload].<br>
     * [4 byte int][4 byte int][4 byte int][2 byte short][2 byte short][4096 byte payload].<br>
     * Phân giải thành nhiều frame nếu dữ liệu vượt quá số byte cho phép.
     * @param rawMessage
     * @return 
     */
    public ByteBuffer[] wrapMessage(String rawMessage){
        Frame.setFrameLength(MAX_PAYLOAD);
        byte[] byteMessage = rawMessage.getBytes(StandardCharsets.UTF_8);
        short numPart = (short) ((short)(byteMessage.length + MAX_PAYLOAD - 1)/MAX_PAYLOAD);
        ByteBuffer[] frames = new ByteBuffer[numPart];        
        short count = 0;
        for(int start=0; start<byteMessage.length; start+=MAX_PAYLOAD){
            int end = Math.min(start+MAX_PAYLOAD, byteMessage.length);
            
            byte[] payload = Arrays.copyOfRange(byteMessage, start, end);
            
            Frame frame = new Frame(sessionId, messageId, numPart, count, payload);
            frames[count] = frame.getFrame();
//            System.out.println(frame);
            
            count++;
        }
        
        return frames;
    }
    
    public String unwrapMessage(){
        int totalLength = 0;
        for(Frame frame: frameList){
            totalLength += frame.getPayload().length;
        }
        byte[] byteMessage = new byte[totalLength];
        
        int pos = 0;
        for(Frame frame: frameList){
            int curLength = frame.getPayload().length;
            System.arraycopy(frame.getPayload(), 0, byteMessage, pos, curLength);
            pos += curLength;
        }
        
        return new String(byteMessage, StandardCharsets.UTF_8);
    }

}



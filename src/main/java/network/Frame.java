package network;

import java.nio.ByteBuffer;

/**
 * Giao thức frame:<br>
 * <br>
 * [byte length][session id][message id][number of part][part id][payload].<br>
 * <br>
 * [4 byte][4 byte][4 byte][2 byte][2 byte][4096 byte].<br>
 */
public class Frame{
    private int sessionId;
    private int messageId;
    private short numPart;
    private short partId;
    private byte[] payload;
    
    private static int FRAME_LENGTH;
    
    public static void setFrameLength(int length){
        FRAME_LENGTH = 16+length;
    }
    
    public static int getFrameLength(){
        return FRAME_LENGTH;
    }

    @Override
    public String toString() {
        return "Frame{" + "sessionId=" + sessionId + ", messageId=" + messageId + ", numPart=" + numPart + ", partId=" + partId + ", payload_length="+ payload.length + '}';
    }

    public Frame(int sessionId, int messageId, short numPart, short partId, byte[] payload) {
        this.sessionId = sessionId;
        this.messageId = messageId;
        this.numPart = numPart;
        this.partId = partId;
        this.payload = payload;
    }
    
    public Frame(ByteBuffer byteBuffer){
        this.sessionId = byteBuffer.getInt();
        this.messageId = byteBuffer.getInt();
        this.numPart = byteBuffer.getShort();
        this.partId = byteBuffer.getShort();
        byte[] payloadByte = new byte[byteBuffer.remaining()];
        byteBuffer.get(payloadByte);     
        this.payload = payloadByte;
    }
    
    public ByteBuffer getFrame(){
        ByteBuffer frame = ByteBuffer.allocate(FRAME_LENGTH);
        frame.putInt(payload.length);
        frame.putInt(sessionId);
        frame.putInt(messageId);
        frame.putShort(numPart);
        frame.putShort(partId);
        frame.put(payload);
        
        frame.flip();
        return frame;
    }

    public byte[] getPayload() {
        return payload;
    }
    
    public short getNumPart() {
        return numPart;
    }
        
}

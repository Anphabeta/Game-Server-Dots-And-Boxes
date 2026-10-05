package request;

import controller.MasterController;
import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import network.NioServer;

public class Decoder {
    private MasterController controller;
    private NioServer network;
    
    private static final Map<String, Consumer<Pair>> functionMap = new HashMap<>();
    
    private class Pair{
        private SelectionKey key;
        private String params;

        public Pair(SelectionKey key, String params) {
            this.key = key;
            this.params = params;
        }

        public SelectionKey getKey() {
            return key;
        }

        public String getParams() {
            return params;
        }
    }

    public Decoder(){
        functionMap.put("PING", pair->ping(pair));
        functionMap.put("CHAT", pair->chat(pair));
//        functionMap.put(key, value);
//        functionMap.put(key, value);
    }
    
    public void setController(MasterController controller) {
        this.controller = controller;
    }

    public void setNetwork(NioServer network) {
        this.network = network;
    }
    
    public void readMessage(SelectionKey key, String rawMessage){
        String[] parts = rawMessage.split(",,", -1);
        String opCode = parts[0];
        String params = parts[1];
        
        Consumer<Pair> handle = functionMap.get(opCode);
        if(handle!=null){
            handle.accept(new Pair(key, params));
        }
        
    }

    public void lostConnection() {
        
    }
    
    private void ping(Pair pair){
        controller.ping(pair.getKey());
    }
    
    private void chat(Pair pair){
        System.out.println(pair.params);
    }
}

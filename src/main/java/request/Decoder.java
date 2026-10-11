package request;

import controller.MasterController;
import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import network.NioServer;

public class Decoder {
    private MasterController controller;
    private NioServer network;
    
    private static final Map<String, Consumer<Pair>> globalMap = new HashMap<>();
    private static final Set<String> individualMap = new HashSet<>();
    
    private class Pair{
        private final SelectionKey key;
        private final String params;
        private final int sessionId;

        public Pair(SelectionKey key, String params, int sessionId) {
            this.key = key;
            this.params = params;
            this.sessionId = sessionId;
        }

        @Override
        public String toString() {
            return "Pair{" + "key=" + key + ", params=" + params + '}';
        }
    }
    
    private void initGlobalFunctionMap(){
        globalMap.put("PING", pair->ping(pair));
        globalMap.put("CHAT", pair->chat(pair));
        globalMap.put("LOGIN", pair->login(pair));
        globalMap.put("REGISTER", pair->register(pair));
        globalMap.put("LOGOUT", pair->logout(pair));
        globalMap.put("ATTACH_REQUEST", pair->attachRequest(pair));
        globalMap.put("INVITE", pair->invite(pair));
//        globalMap.put(key, value);
//        globalMap.put(key, value);
        globalMap.put("CHAT_BROADCAST", pair->chatBroadcast(pair));  
        globalMap.put("CHAT_INDVD", pair->chatIndividual(pair));
    }
    
    private void initIndividualFunctionMap(){
        individualMap.add("GET_USER_DETAIL");        
        individualMap.add("JOIN_QUICK_MATCH");
        individualMap.add("INVITE");
        individualMap.add("CREATE_CUSTOM_ROOM");
    }

    public Decoder(){
        initGlobalFunctionMap();
        initIndividualFunctionMap();
    }
    
    public void setController(MasterController controller) {
        this.controller = controller;
    }

    public void setNetwork(NioServer network) {
        this.network = network;
    }
    
    public void readMessage(SelectionKey key, String rawMessage, int sessionId){
//        System.out.println("Da toi decoder");
        try {
            String[] parts = rawMessage.split(",,", 2);
            String opCode = parts[0];
            String params = parts[1];
            
            if(individualMap.contains(opCode)){
                controller.dispatchRepresentative(sessionId, opCode, params, key);
            }
            else if(globalMap.containsKey(opCode)){
                Consumer<Pair> handle = globalMap.get(opCode);
                if(handle!=null){
                    handle.accept(new Pair(key, params, sessionId));
                }                
            }
            else{
                showInvalid(new Pair(key, params, sessionId));                
            }
        } catch (Exception e) {
            System.out.println(e);
            return;
        }

    }
    
    // General request
    
    private void ping(Pair pair){
        controller.ping(pair.key, pair.sessionId);
    }
    
    private void chat(Pair pair){
        System.out.println(String.format("Client %d: %s", pair.sessionId, pair.params));
    }
    
    private void login(Pair pair){
        String[] param = pair.params.split(";");
        if(param.length!=2) return;
        controller.login(pair.key, param[0], param[1]);
    }
    
    private void register(Pair pair){
        String[] param = pair.params.split(";");
        if(param.length!=3) return;
        controller.register(pair.key, param[0], param[1], param[2]);
    }
    
    private void logout(Pair pair){
        int sessionId;
        try {
            sessionId = Integer.parseInt(pair.params);
            controller.logout(pair.key, sessionId);
        } catch (NumberFormatException e) {
            System.out.println(e);
        }
    }
    
    private void attachRequest(Pair pair){
        int sessionId;
        try {
            sessionId = Integer.parseInt(pair.params);
            controller.attachRequest(pair.key, sessionId);
        } catch (NumberFormatException e) {
            System.out.println(e);
        }
    }
    
    private void invite(Pair pair){
        
    }
    
    
    // Other request
    
    private void chatBroadcast(Pair pair){
        controller.chatBroadcast(pair.params);
    }
    
    private void chatIndividual(Pair pair){
        try {
            String[] parameter = pair.params.split(";");
            int desUserId = Integer.parseInt(parameter[0]);
            String message = parameter[1];
            controller.chatIndividual(pair.sessionId, desUserId, message);
        } catch (Exception e) {
            System.out.println("Invalid params");
        }
    }
    
    private void showInvalid(Pair pair){
        System.out.println("Invalid message");
        System.out.println(pair);
    }
}

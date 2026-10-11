package com.ltm_n11.main;

import java.util.Scanner;
import request.Decoder;
import request.Encoder;

public class Comunication extends Thread{
    private final Decoder decoder;
    private final Encoder encoder;

    public Comunication(Decoder decoder, Encoder encoder) {
        this.decoder = decoder;
        this.encoder = encoder;
    }

    
    
    @Override
    public void run(){
        Scanner sc = new Scanner(System.in);
        
        while(true){
            String send = sc.nextLine();
            if(send.strip().equals("EXIT")){
                break;
            }
            
            if(send.split(",,")[0].equals("CHAT_BROADCAST")){
                encoder.chatBroadcast(send.split(",,")[1]);
            }
            else{
                decoder.readMessage(null, send, -1);                
            }

        }
    }
        
}

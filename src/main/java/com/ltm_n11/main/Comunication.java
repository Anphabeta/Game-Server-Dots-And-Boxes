package com.ltm_n11.main;

import java.util.Scanner;
import network.NioServer;
import request.Encoder;

public class Comunication extends Thread{
    private final Encoder encoder;

    public Comunication(Encoder encoder) {
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
            
            encoder.chatBroadcast(send);
        }
    }
        
}

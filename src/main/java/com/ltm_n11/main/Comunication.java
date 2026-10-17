package com.ltm_n11.main;

import dao.MatchDAO;
import dao.UserDAO;
import java.util.Scanner;
import request.Decoder;
import request.Encoder;

public class Comunication extends Thread{
    private final Decoder decoder;
    private final Encoder encoder;
    private final MatchDAO matchDAO = new MatchDAO();
//    private final UserDAO userDAO = new UserDAO();

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
            if(send.split(",,")[0].equals("SHOW")){
                String param = send.split(",,")[1];
                if(param.equals("Match")){
                    var matchs = matchDAO.getMatchesByUserId(1);
                    for(var match: matchs){
                        System.out.println(match);
                    }
                }
            }
            else{
                decoder.readMessage(null, send, -1);                
            }

        }
    }
        
}

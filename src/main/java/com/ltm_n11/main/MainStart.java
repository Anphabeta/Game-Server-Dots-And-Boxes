package com.ltm_n11.main;

import controller.MasterController;
import dao.MatchDAO;
import dao.UserDAO;
import java.io.IOException;
import network.NioServer;
import request.Decoder;
import request.Encoder;

public class MainStart {
    private static final NioServer nioServer = new NioServer(5000);
    private static final Decoder decoder = new Decoder();
    private static final Encoder encoder = new Encoder();
    private static final MasterController controller = new MasterController();
    
    private static final Comunication comunication = new Comunication(decoder, encoder);
    
    public static void asignDependency(){
        nioServer.setDecoder(decoder);
        decoder.setController(controller);
        encoder.setNetwork(nioServer);
        controller.setEncoder(encoder);
    }
    
    public static void run(){
        try {
            comunication.start();
            
            nioServer.listenSocket();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}

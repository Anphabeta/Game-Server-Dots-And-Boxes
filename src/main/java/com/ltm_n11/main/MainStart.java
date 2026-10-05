package com.ltm_n11.main;

import controller.MasterController;
import dao.MatchDAO;
import dao.UserDAO;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import network.NioServer;
import request.Decoder;
import request.Encoder;

public class MainStart {
    private static final NioServer nioServer = new NioServer(5000);
    private static final Decoder decoder = new Decoder();
    private static final Encoder encoder = new Encoder();
    private static final MasterController controller = new MasterController();
    private static final UserDAO userDAO = new UserDAO();
    private static final MatchDAO matchDAO = new MatchDAO();
    
    public static void asignDependency(){
        nioServer.setDecoder(decoder);
        decoder.setController(controller);
        controller.setEncoder(encoder);
        controller.setMatchDAO(matchDAO);
        controller.setUserDAO(userDAO);
    }
    
    public static void run(){
        try {
            nioServer.listenSocket();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}

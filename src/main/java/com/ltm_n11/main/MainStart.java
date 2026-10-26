package com.ltm_n11.main;

import java.io.IOException;

import controller.MasterController;
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
    
    public static void run() {
        try {
            // Reset trạng thái của các phiên từ lần chạy trước
            controller.resetStaleUserStatuses();

            // Bắt đầu các luồng server sau khi reset
            comunication.start();
            nioServer.listenSocket();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}

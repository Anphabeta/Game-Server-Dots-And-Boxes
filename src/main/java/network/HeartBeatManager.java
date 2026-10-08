package network;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
/*
T cần
Lúc khởi tạo, tạo một schedule
Nếu nhận trước 5s, reset schedule
Nếu hết 5s, gửi ping
*/
public class HeartBeatManager {
    private ScheduledExecutorService scheduler;

    private final Runnable sendPing;
    private final Runnable lostConnection;
    private final long time;
    
    private long lastReceiveMes = System.currentTimeMillis();

    public HeartBeatManager(Runnable sendPing, Runnable lostConnection, long time) {
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.sendPing = sendPing;
        this.lostConnection = lostConnection;
        this.time = time;
        startHeartBeat();
    }
    
    public void resetSchedule(){
        lastReceiveMes = System.currentTimeMillis();
    }
    
    private void handleTime(){
        long idle = System.currentTimeMillis() - lastReceiveMes;
        if(idle>15000){
            lostConnection.run();
        }
        else if(idle>5000){
            sendPing.run();
        }
    }
    
    private void startHeartBeat(){
        scheduler.scheduleAtFixedRate(() -> handleTime(), time, time, TimeUnit.SECONDS);
    }
    
    public void stop(){
        if(scheduler!=null && !scheduler.isShutdown()){
            scheduler.shutdownNow();
        }
    }
}

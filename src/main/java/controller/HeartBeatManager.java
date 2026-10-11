package controller;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
/*
T cần
Lúc khởi tạo, tạo một schedule
Nếu nhận trước 5s, reset schedule
Nếu hết 5s, gửi ping
*/
public class HeartBeatManager {
    private ScheduledExecutorService scheduler;

    private final Runnable removeSelf;
    
    private long lastPing = System.currentTimeMillis();

    public HeartBeatManager(Runnable removeSelf, long time) {
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.removeSelf = removeSelf;
        scheduler.scheduleAtFixedRate(() -> handleTime(), time, time, TimeUnit.SECONDS);        
    }
    
    public void resetSchedule(){
        System.out.println("Reset schedule");
        lastPing = System.currentTimeMillis();
    }
    
    private void handleTime(){
        long idle = System.currentTimeMillis() - lastPing;
        if(idle>120000){
            removeSelf.run();
        }
    }
    
    public void stop(){
        if(scheduler!=null && !scheduler.isShutdown()){
            scheduler.shutdownNow();
        }
    }
}

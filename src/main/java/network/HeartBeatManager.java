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
    
    private int missingPings = 0;
    private ScheduledFuture<?> timeoutTask;
    
    private final Runnable task;
    private final Runnable whenTimeout;
    private final long time;

    public HeartBeatManager(Runnable task, Runnable whenTimeout, long time) {
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.task = task;
        this.whenTimeout = whenTimeout;
        this.time = time;
        this.timeoutTask = scheduler.schedule(()->timeout(), time, TimeUnit.SECONDS);
    }
    
    public void timeout(){
        task.run();
        missingPings++;
        if(missingPings>=3){
            whenTimeout.run();
            return;
        }
        
        timeoutTask = scheduler.schedule(()->timeout(), time, TimeUnit.SECONDS);
    }
    
    public void resetSchedule(){
        if(timeoutTask!=null){
            timeoutTask.cancel(false);
        }
        
        missingPings = 0;
        
        timeoutTask = scheduler.schedule(()->timeout(), time, TimeUnit.SECONDS);
        
    }
}

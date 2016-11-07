package org.farmer.potato.pool;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

/**
 * 土豆收割者
 * User: zhangjiajun2
 * Date: 2016/11/7
 * Time: 15:34
 */
public class PotatoReaper implements Runnable {

    private final PotatoFarmland farmland; //土豆地

    private ScheduledExecutorService reaperScheduledExecutor;

    private String potatoName; //土豆名

    private Throwable throwable;

    private long sickDetectionThreshold; //生病判断阀值，超过阀值设定时间不再生长认为生病

    private boolean isSick;    //是否生病

    private ScheduledFuture<?> scheduledFuture;

    public PotatoReaper(PotatoFarmland farmland) {
        this.farmland = farmland;
    }

    @Override
    public void run() {
        isSick = true;
    }
}

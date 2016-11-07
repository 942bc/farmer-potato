package org.farmer.potato.pool;

import org.farmer.potato.PotatoConfig;
import org.farmer.potato.PotatoFarmlandMXBean;
import org.farmer.potato.util.PoolStateLock;
import org.farmer.potato.util.FarmTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * 土豆地
 * User: zhangjiajun2
 * Date: 2016/11/6
 * Time: 17:02
 */
public class PotatoFarmland extends BasePool implements PotatoFarmlandMXBean {

    private final Logger logger = LoggerFactory.getLogger(PotatoFarmland.class);

    private static final byte POOL_STATE_NORMAL = 0;  //正常
    private static final byte POOL_STATE_PAUSED = 1;  //挂起
    private static final byte POOL_STATE_CLOSED = -1; //关闭

    private static final long FEED_PERIOD_MS = 30000; //施肥周期，单位ms

    private volatile byte poolState;

    private final ExecutorService addConnExecutor;

    private final ExecutorService closeConnExecutor;

    private ScheduledThreadPoolExecutor feedExecutorService;

    private PoolStateLock stateLock;

    private ScheduledFuture<?> feedTask; //施肥任务


    public PotatoFarmland(final PotatoConfig config){
        super(config);
        ThreadFactory threadFactory = config.getThreadFactory();
        this.addConnExecutor = FarmTools.createThreadPoolExecutor(config.getMaxPoolSize(), poolName + " conn adder", threadFactory, new ThreadPoolExecutor.DiscardPolicy());
        this.closeConnExecutor = FarmTools.createThreadPoolExecutor(config.getMaxPoolSize(), poolName + " conn closer", threadFactory, new ThreadPoolExecutor.CallerRunsPolicy());
        this.feedExecutorService = config.getScheduledExecutor();
        if (this.feedExecutorService == null) {
            threadFactory = threadFactory != null ? threadFactory : new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, poolName + " keeper");
                    thread.setDaemon(true);
                    return thread;
                }
            };
            this.feedExecutorService = new ScheduledThreadPoolExecutor(1, threadFactory, new ThreadPoolExecutor.DiscardPolicy());
            this.feedExecutorService.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        }
        this.stateLock = new PoolStateLock(config.isAllowPoolPause());

        //施肥保活
        this.feedTask = this.feedExecutorService.scheduleWithFixedDelay(
                new PotatoFertilizer(this),
                100L, FEED_PERIOD_MS, TimeUnit.MILLISECONDS);

        //收割生病的土豆


    }

    @Override
    public int getIdleConnections() {
        return 0;
    }

    @Override
    public int getActiveConnections() {
        return 0;
    }

    @Override
    public int getTotalConnections() {
        return 0;
    }

    @Override
    public int getThreadsAwaitingConnection() {
        return 0;
    }

    @Override
    public void softEvictConnections() {

    }

    @Override
    public void pausePool() {

    }

    @Override
    public void resumePool() {

    }
}

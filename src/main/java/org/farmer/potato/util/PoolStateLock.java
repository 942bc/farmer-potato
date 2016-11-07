package org.farmer.potato.util;

import java.util.concurrent.Semaphore;

/**
 * User: zhangjiajun2
 * Date: 2016/11/7
 * Time: 14:43
 * 连接池状态锁
 */
public class PoolStateLock {

    private static final int MAX_PERMITS = 4000;
    private final Semaphore acquisitionSemaphore;
    private boolean isSemaphore = false;

    public PoolStateLock(){
        this(true);
    }

    public PoolStateLock(boolean semaphore) {
        acquisitionSemaphore = semaphore ? new Semaphore(MAX_PERMITS, true) : null;
        this.isSemaphore = false;
    }

    public void acquire(){
        if (isSemaphore)
            acquisitionSemaphore.acquireUninterruptibly();
    }

    public void release(){
        if (isSemaphore)
            acquisitionSemaphore.release();
    }
    public void pause(){
        if (isSemaphore)
            acquisitionSemaphore.acquireUninterruptibly(MAX_PERMITS);
    }
    public void resume(){
        if (isSemaphore)
            acquisitionSemaphore.release(MAX_PERMITS);
    }
}

package org.farmer.potato.util;

import java.util.concurrent.ThreadFactory;

/**
 * Created with IntelliJ IDEA.
 * User: zhangjiajun2
 * Date: 2016/11/6
 * Time: 16:55
 */
public class DefaultThreadFactory implements ThreadFactory {
    private final String threadName;
    private final boolean daemon;

    public DefaultThreadFactory(String threadName, boolean daemon) {
        this.daemon = daemon;
        this.threadName = threadName;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, threadName);
        thread.setDaemon(daemon);
        return thread;
    }
}

package org.farmer.potato;

/**
 * Created by zhangjiajun2 on 2016/11/6.
 */
public interface PotatoFarmlandMXBean {
    /**
     * 获取空闲连接数量
     * @return
     */
    int getIdleConnections();

    /**
     * 获取活动连接数量
     * @return
     */
    int getActiveConnections();

    /**
     * 获取总连接数量
     * @return
     */
    int getTotalConnections();

    /**
     * 获取等待连接的线程数量
     * @return
     */
    int getThreadsAwaitingConnection();

    void softEvictConnections();

    /**
     * 挂起连接池
     */
    void pausePool();

    /**
     * 恢复连接池
     */
    void resumePool();
}

package org.farmer.potato.pool;

/**
 * 土豆收割者
 * User: zhangjiajun2
 * Date: 2016/11/7
 * Time: 15:34
 */
public class PotatoReaper implements Runnable {

    private final PotatoFarmland pool;

    public PotatoReaper(PotatoFarmland pool) {
        this.pool = pool;
    }

    @Override
    public void run() {
        final long connectionTimeout = pool.potatoConfig.getConnectionTimeout();
    }
}

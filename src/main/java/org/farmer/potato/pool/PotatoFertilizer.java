package org.farmer.potato.pool;

/**
 * 施肥人（连接保活）
 * User: zhangjiajun2
 * Date: 2016/11/7
 * Time: 15:31
 */
public class PotatoFertilizer implements Runnable {

    private final PotatoFarmland pool;

    PotatoFertilizer(PotatoFarmland pool) {
        this.pool = pool;
    }

    @Override
    public void run() {

    }
}

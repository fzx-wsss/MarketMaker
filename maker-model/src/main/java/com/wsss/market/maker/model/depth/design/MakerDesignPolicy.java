package com.wsss.market.maker.model.depth.design;


import com.wsss.market.maker.model.depth.thread.DesignOrderTask;

public interface MakerDesignPolicy {
    /**
     * 正常情况下，应该如何下撤单
     * @return
     */
    DesignOrderTask designOrder();

    DesignType getDesignType();
}

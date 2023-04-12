package com.wsss.market.maker.depth.design;

import com.wsss.market.maker.depth.thread.DesignOrderTask;

public interface MakerDesignPolicy {
    /**
     * 正常情况下，应该如何下撤单
     * @return
     */
    DesignOrderTask designOrder();

    DesignType getDesignType();
}

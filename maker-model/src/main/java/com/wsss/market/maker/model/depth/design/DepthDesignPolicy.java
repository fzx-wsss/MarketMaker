package com.wsss.market.maker.model.depth.design;


public interface DepthDesignPolicy {
    /**
     * 正常情况下，应该如何下撤单
     * @return
     */
    MakerContext designOrder();

    DepthDesignType getDesignType();
}

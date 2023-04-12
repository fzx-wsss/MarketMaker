package com.wsss.market.maker.depth.design;

import com.wsss.market.maker.depth.thread.DesignOrderTask;

public class TriangleMakerDesignPolicy implements MakerDesignPolicy {
    @Override
    public DesignOrderTask designOrder() {
        return null;
    }

    @Override
    public DesignType getDesignType() {
        return DesignType.TRIANGLE;
    }
}

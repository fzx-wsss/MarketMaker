package com.wsss.market.maker.domain;

import java.math.BigDecimal;
import java.util.Comparator;

public enum Side {
    BUY(new Comparator<BigDecimal>() {
        @Override
        public int compare(BigDecimal o1, BigDecimal o2) {
            return o2.compareTo(o1);
        }
    }){},
    SELL(new Comparator<BigDecimal>() {
        @Override
        public int compare(BigDecimal o1, BigDecimal o2) {
            return o1.compareTo(o2);
        }
    }){};

    Side(Comparator<BigDecimal> comparator) {
        this.comparator = comparator;
    }

    public Comparator<BigDecimal> comparator;

    public Comparator<BigDecimal> getComparator() {
        return this.comparator;
    }

    /**
     * 按照订单簿的顺序判断，p2是否在p1的后面
     * 买二在买一后面
     * 卖二在卖一后面
     * 以此类推
     * @param p1
     * @param p2
     * @return
     */
    public boolean isAfter(BigDecimal p1,BigDecimal p2) {
        return this.comparator.compare(p1,p2) < 0;
    }
}

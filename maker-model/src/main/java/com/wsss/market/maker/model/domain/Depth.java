package com.wsss.market.maker.model.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@ToString
public class Depth {
    private Side side;
    private BigDecimal price;
    private BigDecimal volume = BigDecimal.ZERO;

    @Builder
    public Depth(Side side, BigDecimal price) {
        this.side = side;
        this.price = price;
    }

    public BigDecimal update(BigDecimal volume) {
        BigDecimal old = this.volume;
        this.volume = volume;
        return old;
    }

    public Depth add(BigDecimal volume) {
        if(BigDecimal.ZERO.compareTo(volume) == 0) {
            return this;
        }
        this.volume = this.volume.add(volume);
        return this;
    }

    public Depth sub(BigDecimal volume) {
        if(BigDecimal.ZERO.compareTo(volume) == 0) {
            return this;
        }
        this.volume = this.volume.subtract(volume);
        return this;
    }

}

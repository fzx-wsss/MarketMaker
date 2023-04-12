package com.wsss.market.maker.domain;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@ToString
public class Depth {
    private Side side;
    private BigDecimal price;
    private BigDecimal volume = BigDecimal.ZERO;
    private Map<Source, BigDecimal> sourceVolume = new ConcurrentHashMap<>();

    @Builder
    public Depth(Side side, BigDecimal price) {
        this.side = side;
        this.price = price;
    }

    public BigDecimal update(Source source, BigDecimal volume) {
        BigDecimal old = sourceVolume.getOrDefault(source, BigDecimal.ZERO);
        this.volume = this.volume.subtract(old).add(volume);

        if (BigDecimal.ZERO.compareTo(volume) == 0) {
            sourceVolume.remove(source);
        } else {
            sourceVolume.put(source, volume);
        }
        return volume;
    }
}

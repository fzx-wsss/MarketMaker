package com.wsss.market.maker.model.domain;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class Order {
    private String orderId;
    private BigDecimal price;
    private BigDecimal volume;
    private Side side;
    private Date ctime = new Date();

    public Order(BigDecimal price, BigDecimal volume, Side side) {
        this.price = price;
        this.volume = volume;
        this.side = side;
    }
}

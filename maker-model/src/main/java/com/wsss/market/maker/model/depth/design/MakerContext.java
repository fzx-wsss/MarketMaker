package com.wsss.market.maker.model.depth.design;

import com.wsss.market.maker.model.domain.Order;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
public class MakerContext {
    // 真实的买一
    private BigDecimal realBestBuy;
    // 真实的买一
    private BigDecimal realBestSell;
    // 让出一些盘口后的买一
    private BigDecimal spreadBestBuy;
    // 让出一些盘口后的卖一
    private BigDecimal spreadBestSell;
    // 用户避让之后的买一
    private BigDecimal makeBestBuy;
    // 用户避让之后的卖一
    private BigDecimal makeBestSell;

    private List<Order> placeOrders = new ArrayList<>();
    private List<Order> removeOrders = new ArrayList<>();


    @Builder
    public MakerContext(BigDecimal realBestBuy, BigDecimal realBestSell, BigDecimal spreadBestBuy, BigDecimal spreadBestSell) {
        this.realBestBuy = realBestBuy;
        this.realBestSell = realBestSell;
        this.spreadBestBuy = spreadBestBuy;
        this.spreadBestSell = spreadBestSell;
        // 默认是按照让出盘口后的价格摆盘，如果发生大单避让则会被覆盖
        this.makeBestBuy = spreadBestBuy;
        this.makeBestSell = spreadBestSell;
    }

    public void addPlaceOrder(Order order) {
        placeOrders.add(order);
    }
    public void addPlaceOrder(Collection<Order> orders) {
        placeOrders.addAll(orders);
    }
    public void addRemoveOrder(Order order) {
        removeOrders.add(order);
    }
    public void addRemoveOrder(Collection<Order> orders) {
        removeOrders.addAll(orders);
    }
}

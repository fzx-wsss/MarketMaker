package com.wsss.market.maker.rpc.impl;

import com.wsss.market.maker.model.domain.Order;
import com.wsss.market.maker.rpc.OrderService;
import javafx.util.Pair;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class EinsteinOrderService implements OrderService {
    @Override
    public void placeOrCancelOrders(String symbol, List<Order> orders) {
        return;
    }

    @Override
    public List<Order> getOpenOrders(String symbol, Integer userId) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Pair<BigDecimal, BigDecimal> getPriceRange(String symbol) {
        return null;
    }
}

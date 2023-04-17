package com.wsss.market.maker.service.task;

import com.wsss.market.maker.model.domain.Order;
import com.wsss.market.maker.model.utils.ApplicationUtils;
import com.wsss.market.maker.rpc.OrderService;
import lombok.Builder;

import java.util.List;
import java.util.concurrent.Callable;

public class QueryOwnerOrderTask implements Callable<List<Order>> {
    private String symbol;
    private Integer uid;
    private OrderService orderService;

    @Builder
    public QueryOwnerOrderTask(String symbol, Integer uid) {
        this.symbol = symbol;
        this.uid = uid;
        this.orderService = ApplicationUtils.getSpringBean(OrderService.class);
    }

    @Override
    public List<Order> call() throws Exception {
        return orderService.getOpenOrders(symbol,uid);
    }
}

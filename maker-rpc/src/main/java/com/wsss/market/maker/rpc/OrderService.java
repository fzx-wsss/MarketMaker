package com.wsss.market.maker.rpc;

import com.wsss.market.maker.model.domain.Order;
import com.wsss.market.maker.model.domain.OrderCommand;
import javafx.util.Pair;

import java.math.BigDecimal;
import java.util.List;

public interface OrderService {

    void placeOrCancelOrders(String symbol, List<OrderCommand> orders);

    List<Order> getOpenOrders(String symbol, Integer userId);

    Pair<BigDecimal, BigDecimal> getPriceRange(String symbol);
}

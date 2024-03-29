package com.wsss.market.maker.inner.supplier.bitrue;

import com.wsss.market.maker.inner.api.place.OrderService;
import com.wsss.market.maker.model.domain.Order;
import com.wsss.market.maker.model.domain.OrderCommand;
import javafx.util.Pair;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Component
public class NewtonOrderService implements OrderService {
    @Override
    public void placeOrCancelOrders(String symbol, List<OrderCommand> orders) {
        return;
    }

    @Override
    public List<Order> getOpenOrders(String symbol, Integer userId) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Pair<BigDecimal, BigDecimal> getPriceRange(String symbol) {
        return new Pair<>(null,null);
    }
}

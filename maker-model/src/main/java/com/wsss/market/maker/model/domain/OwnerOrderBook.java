package com.wsss.market.maker.model.domain;

import com.wsss.market.maker.model.domain.maker.Operation;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

@Component
@Scope("prototype")
public class OwnerOrderBook extends AbstractOrderBook<Set<Order>> {

    public OwnerOrderBook(SymbolInfo symbolInfo) {
        this.symbolInfo = symbolInfo;
    }

    public void update(Order order, Operation operation) {
        ConcurrentSkipListMap<BigDecimal,Set<Order>> map = order.getSide() == Side.BUY ? buys: sells;
        Set<Order> set = map.get(order.getPrice());
        if(set == null) {
            set = map.computeIfAbsent(order.getPrice(),k->new HashSet<>());
        }
        if(operation == Operation.PLACE) {
            set.add(order);
        }else if(operation == Operation.CANCEL) {
            set.remove(order);
            if(set.isEmpty()) {
                map.remove(order.getPrice());
            }
        }

    }
}

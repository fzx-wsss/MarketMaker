package com.wsss.market.maker.service.task;

import com.wsss.market.maker.model.domain.Order;
import com.wsss.market.maker.model.domain.OwnerOrderBook;
import com.wsss.market.maker.model.domain.Side;
import com.wsss.market.maker.model.domain.SymbolInfo;
import com.wsss.market.maker.model.domain.maker.Operation;
import com.wsss.market.maker.model.utils.ApplicationUtils;
import com.wsss.market.maker.rpc.OrderService;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class QueryOwnerOrderTask extends AbstractAsyncTask<Boolean> {
    private Integer uid;
    private Side side;
    private OwnerOrderBook orderBook;
    private OrderService orderService;

    @Builder
    public QueryOwnerOrderTask(SymbolInfo symbol) {
        super(symbol);
        this.uid = uid;
        this.side = side;
        this.orderService = ApplicationUtils.getSpringBean(OrderService.class);
        this.orderBook = symbol.getOwnerOrderBook();
    }

    @Override
    public Boolean doCall() throws Exception {
        List<Order> list = orderService.getOpenOrders(symbolInfo.getSymbol(),uid);
        if(list.isEmpty()) {
            return true;
        }

        list.forEach(o->{
            if(o.getSide() != side) {
                log.error("order side is error:{}",o.getOrderId());
                return;
            }
            if(orderBook.getBook(o.getPrice(),o.getSide()) != null && !isRecentOrder(o)) {
                orderBook.update(o,Operation.PLACE);
            }
        });

        Set<String> set = list.stream().map(o->o.getOrderId()).collect(Collectors.toSet());
        List<Order> orders = orderBook.stream(side).map(e->e.getValue()).flatMap(Collection::stream).collect(Collectors.toList());
        orders.forEach(o-> {
            if(!set.contains(o.getOrderId()) && !isRecentOrder(o)) {
                orderBook.update(o,Operation.PLACE);
            }
        });
        return true;
    }

    private boolean isRecentOrder(Order order) {
        return (System.currentTimeMillis() - order.getCtime().getTime()) < TimeUnit.SECONDS.toMillis(symbolInfo.getMakerConfig().getMinSyncTime());
    }
}

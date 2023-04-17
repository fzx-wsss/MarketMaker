package com.wsss.market.maker.service.task;


import com.wsss.market.maker.model.domain.Order;
import com.wsss.market.maker.model.domain.OwnerOrderBook;
import com.wsss.market.maker.model.domain.SymbolInfo;
import com.wsss.market.maker.model.domain.maker.Operation;
import com.wsss.market.maker.model.utils.ApplicationUtils;
import com.wsss.market.maker.rpc.OrderService;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Getter
public class DesignOrderTask implements Callable<Boolean> {
    private volatile boolean finish = false;
    private SymbolInfo symbolInfo;
    private List<Order> cancelOrderList;
    private List<Order> placeOrderList;
    private OrderService orderService;
    private long time = System.currentTimeMillis();

    @Builder
    public DesignOrderTask(SymbolInfo symbolInfo, List<Order> cancelOrderList, List<Order> placeOrderList) {
        this.symbolInfo = symbolInfo;
        this.cancelOrderList = cancelOrderList;
        this.placeOrderList = placeOrderList;
        this.orderService = ApplicationUtils.getSpringBean(OrderService.class);
    }

    @Override
    public Boolean call() throws Exception {
        designOrder();
        updateOrderBook();
        finish();
        return true;
    }

    public void designOrder() {
        try {
            List<Order> list = Stream.concat(placeOrderList.stream(),cancelOrderList.stream()).collect(Collectors.toList());
            Collections.shuffle(list);
            orderService.placeOrCancelOrders(symbolInfo.getSymbol(), list);
            if(symbolInfo.isDebugLog()) {
                log.info("placeOrderList size:{},cancelOrderList size:{}",placeOrderList.size(),cancelOrderList.size());
            }

        }catch (Exception e) {
            log.error("error",e);
        }
    }

    private void updateOrderBook() {
        OwnerOrderBook orderBook = symbolInfo.getOwnerOrderBook();
        for(Order order : placeOrderList) {
            orderBook.update(order, Operation.PLACE);
        }
        for(Order order : cancelOrderList) {
            orderBook.update(order, Operation.CANCEL);
        }
    }

    public void finish() {
        finish = true;
    }

    public boolean isFinish() {
        return finish;
    }


}

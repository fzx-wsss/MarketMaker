package com.wsss.market.maker.service.task;


import com.superatomfin.framework.monitor.Monitor;
import com.wsss.market.maker.model.domain.Order;
import com.wsss.market.maker.model.domain.OrderCommand;
import com.wsss.market.maker.model.domain.OwnerOrderBook;
import com.wsss.market.maker.model.domain.SymbolInfo;
import com.wsss.market.maker.model.domain.maker.Operation;
import com.wsss.market.maker.model.utils.ApplicationUtils;
import com.wsss.market.maker.model.utils.Perf;
import com.wsss.market.maker.rpc.OrderService;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Getter
public class MakeOrderTask extends AbstractAsyncTask<Boolean> {

    private List<Order> cancelOrderList;
    private List<Order> placeOrderList;
    private static OrderService orderService = ApplicationUtils.getSpringBean(OrderService.class);
    private long time = System.currentTimeMillis();

    @Builder
    public MakeOrderTask(SymbolInfo symbolInfo, List<Order> cancelOrderList, List<Order> placeOrderList) {
        super(symbolInfo);
        this.cancelOrderList = cancelOrderList;
        this.placeOrderList = placeOrderList;
    }

    @Override
    public Boolean doCall() throws Exception {
        Monitor.TimeContext context = Monitor.timer("make_order_task");
        try {
            designOrder();
            updateOrderBook();
            return true;
        } finally {
            context.end();
        }
    }

    public void designOrder() {
        List<OrderCommand> list = new ArrayList<>(placeOrderList.size() + cancelOrderList.size());
        placeOrderList.forEach(o->list.add(OrderCommand.builder().operation(Operation.PLACE).build()));
        cancelOrderList.forEach(o->list.add(OrderCommand.builder().operation(Operation.CANCEL).build()));
        Collections.shuffle(list);
        Perf.count("place_order_num",symbolInfo);
        Perf.count("cancel_order_num",symbolInfo);
        orderService.placeOrCancelOrders(symbolInfo.getSymbol(), list);
        if(symbolInfo.isDebugLog()) {
            log.info("placeOrderList size:{},cancelOrderList size:{}",placeOrderList.size(),cancelOrderList.size());
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

}

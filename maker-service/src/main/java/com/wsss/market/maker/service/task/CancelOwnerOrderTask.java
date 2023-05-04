package com.wsss.market.maker.service.task;

import com.superatomfin.share.tools.other.Sleep;
import com.wsss.market.maker.model.config.MakerConfig;
import com.wsss.market.maker.model.domain.*;
import com.wsss.market.maker.model.domain.maker.Operation;
import com.wsss.market.maker.model.utils.ApplicationUtils;
import com.wsss.market.maker.rpc.OrderService;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class CancelOwnerOrderTask extends AbstractAsyncTask<Boolean> {
    private static OrderService orderService = ApplicationUtils.getSpringBean(OrderService.class);
    private static MakerConfig makerConfig = MakerConfig.getInstance();
    private OwnerOrderBook orderBook;

    @Builder
    public CancelOwnerOrderTask(SymbolInfo symbolInfo) {
        super(symbolInfo);
        this.orderBook = symbolInfo.getOwnerOrderBook();
    }

    @Override
    protected Boolean doCall() throws Exception {
        cancelOrderByBook();
        Sleep.sleepSeconds(makerConfig.getCancelOrderSleep());
        cancelOrderByUid(symbolInfo.getSymbolAo().getOffsetBuyRobotId().intValue());
        cancelOrderByUid(symbolInfo.getSymbolAo().getOffsetSellRobotId().intValue());
        log.info("cancel all order finish");
        return true;
    }

    private void cancelOrderByBook() {
        if(orderBook != null) {
            return;
        }

        Sleep.sleepSeconds(makerConfig.getCancelOrderSleep());
        List<Order> list = new ArrayList<>(orderBook.size());
        orderBook.stream(Side.BUY).forEach(e->{
            list.addAll(e.getValue());
        });
        orderBook.stream(Side.SELL).forEach(e->{
            list.addAll(e.getValue());
        });
        List<OrderCommand> orderCommands = list.stream().map(o-> OrderCommand.builder().order(o).operation(Operation.CANCEL).build()).collect(Collectors.toList());
        log.info("{} cancel order size:{}",orderCommands.size());
        orderService.placeOrCancelOrders(symbolInfo.getSymbol(), orderCommands);
        orderBook.clearAll();
    }

    private void cancelOrderByUid(int uid) {
        List<Order> list = orderService.getOpenOrders(symbolInfo.getSymbol(), uid);
        while(!list.isEmpty()) {
            log.warn("cancel order still exist, uid:{},size:{}",uid,list.size());
            List<OrderCommand> orderCommands = list.stream().map(o-> OrderCommand.builder().order(o).operation(Operation.CANCEL).build()).collect(Collectors.toList());
            orderService.placeOrCancelOrders(symbolInfo.getSymbol(), orderCommands);
            Sleep.sleepSeconds(makerConfig.getCancelOrderSleep());
            list = orderService.getOpenOrders(symbolInfo.getSymbol(), uid);
        }
    }
}

package com.wsss.market.maker.depth.thread;

import com.wsss.market.maker.domain.Order;
import com.wsss.market.maker.domain.OwnerOrderBook;
import com.wsss.market.maker.domain.SymbolInfo;
import com.wsss.market.maker.domain.maker.Operation;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Getter
public class DesignOrderTask implements Runnable {
    private volatile boolean finish = false;
    private SymbolInfo symbolInfo;
    private List<Order> cancelOrderList;
    private List<Order> placeOrderList;
    private long time = System.currentTimeMillis();

    @Builder
    public DesignOrderTask(SymbolInfo symbolInfo, List<Order> cancelOrderList, List<Order> placeOrderList) {
        this.symbolInfo = symbolInfo;
        this.cancelOrderList = cancelOrderList;
        this.placeOrderList = placeOrderList;
    }

    @Override
    public void run() {
        designOrder();
        finish();
    }

    public void designOrder() {
        try {
            OwnerOrderBook orderBook = symbolInfo.getOwnerOrderBook();
            for(Order order : placeOrderList) {
                orderBook.update(order, Operation.PLACE);
            }
            for(Order order : cancelOrderList) {
                orderBook.update(order, Operation.CANCEL);
            }

            if(symbolInfo.isDebugLog()) {
                log.info("placeOrderList size:{},cancelOrderList size:{}",placeOrderList.size(),cancelOrderList.size());
            }

        }catch (Exception e) {
            log.error("error",e);
        }
    }

    public void finish() {
        finish = true;
    }

    public boolean isFinish() {
        return finish;
    }
}

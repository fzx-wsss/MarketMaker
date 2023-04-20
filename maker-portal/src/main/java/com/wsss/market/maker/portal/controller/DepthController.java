package com.wsss.market.maker.portal.controller;

import com.wsss.market.maker.model.domain.SubscribedOrderBook;
import com.wsss.market.maker.model.domain.SymbolInfo;
import com.wsss.market.maker.model.utils.ApplicationUtils;
import com.wsss.market.maker.service.center.DataCenter;
import com.wsss.market.maker.service.task.CancelOwnerOrderTask;
import com.wsss.market.maker.service.thread.pool.MarkerMakerThreadPool;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
@RestController
public class DepthController {

    @Resource
    private DataCenter dataCenter;
    @Resource
    private MarkerMakerThreadPool makerPool;

    @RequestMapping(value = "/query/symbol", method = RequestMethod.GET)
    @ResponseBody
    public Object queryDepth(String symbol) {
        SymbolInfo symbolInfo = dataCenter.getSymbolInfo(symbol);
        if(symbolInfo == null) {
            return Collections.EMPTY_LIST;
        }
        List<Pair> list = new ArrayList<>();
        list.add(new Pair<>(symbolInfo.getOwnerOrderBook().getBuyPrices(),symbolInfo.getOwnerOrderBook().getSellPrices()));
        for(String child : symbolInfo.getChildSymbol()) {
            SubscribedOrderBook book = symbolInfo.getChildSubscribedOrderBook(child);
            list.add(new Pair<>(book.getBuyPrices(),book.getSellPrices()));
        }
        return list;
    }

    @RequestMapping(value = "/cancel/symbol", method = RequestMethod.POST)
    @ResponseBody
    public boolean cancelOrder(@RequestBody Set<String> symbols) {
        symbols.forEach(s->{
            SymbolInfo symbolInfo = dataCenter.getSymbolInfo(s);
            if(symbolInfo == null) {
                symbolInfo = ApplicationUtils.getSpringBean(SymbolInfo.class, s);
            }
            CancelOwnerOrderTask orderTask = CancelOwnerOrderTask.builder().symbolInfo(symbolInfo).build();
            makerPool.execAsyncTask(orderTask);
        });
        return true;
    }

}

package com.wsss.market.maker.portal.controller;

import com.wsss.market.maker.model.center.DataCenter;
import com.wsss.market.maker.model.domain.SubscribedOrderBook;
import com.wsss.market.maker.model.domain.SymbolInfo;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
public class DepthController {

    @Resource
    private DataCenter dataCenter;

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

}

package com.wsss.market.maker.service.task;

import com.wsss.market.maker.model.domain.SymbolInfo;
import com.wsss.market.maker.model.domain.Trade;
import com.wsss.market.maker.model.utils.ApplicationUtils;
import com.wsss.market.maker.model.utils.Perf;
import com.wsss.market.maker.rpc.OrderService;
import com.wsss.market.maker.rpc.TradeService;
import javafx.util.Pair;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class MakeTradeTask extends AbstractAsyncTask<Boolean> {
    private List<Trade> trades;
    private static TradeService tradeService = ApplicationUtils.getSpringBean(TradeService.class);
    private static OrderService orderService = ApplicationUtils.getSpringBean(OrderService.class);

    @Builder
    public MakeTradeTask(SymbolInfo symbolInfo,List<Trade> trades) {
        super(symbolInfo);
        this.trades = trades;
    }

    @Override
    protected Boolean doCall() throws Exception {
        Pair<BigDecimal,BigDecimal> pair = orderService.getPriceRange(symbolInfo.getSymbol());
        BigDecimal buy = pair.getKey();
        BigDecimal sell = pair.getValue();
        trades = trades.stream().filter(trade ->
            (buy == null || buy.compareTo(trade.getPrice()) <= 0) && (sell == null || sell.compareTo(trade.getPrice()) >= 0)
        ).collect(Collectors.toList());
        tradeService.save(trades);
        Perf.count("make_trade_msg",symbolInfo,trades.size());
        return true;
    }
}

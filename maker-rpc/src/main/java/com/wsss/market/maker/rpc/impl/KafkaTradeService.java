package com.wsss.market.maker.rpc.impl;

import com.wsss.market.maker.model.domain.Trade;
import com.wsss.market.maker.rpc.TradeService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KafkaTradeService implements TradeService {
    @Override
    public boolean save(List<Trade> list) {
        return true;
    }
}

package com.wsss.market.maker.rpc;

import com.wsss.market.maker.model.domain.Trade;

import java.util.List;

public interface TradeService {
    boolean save(List<Trade> list);
}

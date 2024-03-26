package com.wsss.market.maker.inner.api.place;

import com.wsss.market.maker.model.domain.Trade;

import java.util.List;

public interface TradeService {
    boolean save(List<Trade> list);
}

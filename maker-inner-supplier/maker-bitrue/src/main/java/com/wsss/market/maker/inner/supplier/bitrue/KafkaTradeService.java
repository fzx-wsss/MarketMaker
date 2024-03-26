package com.wsss.market.maker.inner.supplier.bitrue;

import com.wsss.market.maker.inner.api.place.TradeService;
import com.wsss.market.maker.model.domain.Trade;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KafkaTradeService implements TradeService {
    @Override
    public boolean save(List<Trade> list) {
        return true;
    }
}

package com.wsss.market.maker.inner.api.receive;

import com.wsss.market.maker.model.domain.SymbolInfo;

public interface ListenTask {
    SymbolInfo getSymbol();
}

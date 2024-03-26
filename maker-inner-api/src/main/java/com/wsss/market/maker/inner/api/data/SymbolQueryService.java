package com.wsss.market.maker.inner.api.data;

import com.wsss.market.maker.model.domain.Source;
import com.wsss.market.maker.model.domain.SymbolInfo;

import java.util.Set;

public interface SymbolQueryService {
    Set<SymbolInfo> getMappingSymbolInfo(Source source, String symbol);
}

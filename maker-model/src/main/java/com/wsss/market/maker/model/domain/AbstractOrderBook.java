package com.wsss.market.maker.model.domain;

import lombok.ToString;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

@ToString
public class AbstractOrderBook<T> {
    protected SymbolInfo symbolInfo;
    protected ConcurrentSkipListMap<BigDecimal,T> buys = new ConcurrentSkipListMap<>(Side.BUY.getComparator());
    protected ConcurrentSkipListMap<BigDecimal,T> sells = new ConcurrentSkipListMap<>(Side.SELL.getComparator());

    public BigDecimal getBestBuy() {
        Map.Entry<BigDecimal,T> entry = buys.firstEntry();
        return entry == null ? null : entry.getKey();
    }
    public BigDecimal getBestSell() {
        Map.Entry<BigDecimal,T> entry = sells.firstEntry();
        return entry == null ? null : entry.getKey();
    }

    public boolean buyOrSellIsEmpty() {
        return buys.isEmpty() || sells.isEmpty();
    }

    public T getBuyBook(BigDecimal startPrice) {
        return buys.get(startPrice);
    }
    public T getSellBook(BigDecimal startPrice) {
        return sells.get(startPrice);
    }

    public T getBook(BigDecimal price,Side side) {
        return side == Side.BUY ? buys.get(price) : sells.get(price);
    }

    public List<T> getBuyBooks(BigDecimal startPrice, int maxLevel) {
        List<T> list = new ArrayList<>(maxLevel);
        for(Map.Entry<BigDecimal,T> entry : buys.entrySet()) {
            if(buys.comparator().compare(entry.getKey(),startPrice) >= 0) {
                list.add(entry.getValue());
                if(list.size() >= maxLevel) {
                    break;
                }
            }
        }
        return list;
    }

    public List<T> getSellBooks(BigDecimal startPrice, int maxLevel) {
        List<T> list = new ArrayList<>(maxLevel);
        for(Map.Entry<BigDecimal,T> entry : sells.entrySet()) {
            if(sells.comparator().compare(entry.getKey(),startPrice) >= 0) {
                list.add(entry.getValue());
                if(list.size() >= maxLevel) {
                    break;
                }
            }
        }
        return list;
    }

    public Set<BigDecimal> getBuyPrices() {
        return buys.keySet();
    }

    public Set<BigDecimal> getSellPrices() {
        return sells.keySet();
    }

    public int size() {
        return buys.size() + sells.size();
    }
}

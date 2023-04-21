package com.wsss.market.maker.model.domain;

import lombok.ToString;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

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

    public T getBuyBook(BigDecimal startPrice) {
        return buys.get(startPrice);
    }
    public T getSellBook(BigDecimal startPrice) {
        return sells.get(startPrice);
    }
    public T getBook(BigDecimal price,Side side) {
        return getMap(side).get(price);
    }

    /**
     * 获取盘口到指定价格的档位数据，包含指定价格
     * @param endPrice
     * @return
     */
    public List<T> getNearerBooks(BigDecimal endPrice,Side side) {
        List<T> list = new ArrayList<>();
        ConcurrentSkipListMap<BigDecimal,T> map = getMap(side);
        for(Map.Entry<BigDecimal,T> entry : map.entrySet()) {
            if(map.comparator().compare(entry.getKey(),endPrice) <= 0) {
                list.add(entry.getValue());
                continue;
            }
            break;
        }
        return list;
    }

    public List<T> getFartherBooks(BigDecimal startPrice, int maxLevel,Side side) {
        List<T> list = new ArrayList<>(maxLevel);
        Map<BigDecimal, T> map = getMap(side).tailMap(startPrice);
        for (Map.Entry<BigDecimal, T> entry : map.entrySet()) {
            list.add(entry.getValue());
            if (list.size() >= maxLevel) {
                break;
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

    public T remove(Side side, BigDecimal price) {
        ConcurrentSkipListMap<BigDecimal,T> map = getMap(side);
        return map.remove(price);
    }

    public void clear() {
        buys.clear();
        sells.clear();
    }
    public Stream<Map.Entry<BigDecimal, T>> stream(Side side) {
        return getMap(side).entrySet().stream();
    }
    public boolean buyOrSellIsEmpty() {
        return buys.isEmpty() || sells.isEmpty();
    }
    public int size() {
        return buys.size() + sells.size();
    }

    protected ConcurrentSkipListMap<BigDecimal,T> getMap(Side side) {
        return side == Side.BUY ? buys : sells;
    }
}

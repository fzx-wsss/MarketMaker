package com.wsss.market.maker.model.domain;

import com.wsss.market.maker.model.config.MakerConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

@Slf4j
@Getter
@Setter
@ToString
@Component
@Scope("prototype")
public class SubscribedOrderBook extends AbstractOrderBook<Depth> {

    @Resource
    private MakerConfig makerConfig;
    private Map<Source, SingleOrderBook> singleOrderBooks = new CacheMap<>(k->new SingleOrderBook());
    public SubscribedOrderBook(SymbolInfo symbolInfo) {
        this.symbolInfo = symbolInfo;
    }

    public List<Source> getSourceByPrice(BigDecimal price,Side side) {
        return singleOrderBooks.entrySet().stream()
                .filter(e->e.getValue().getBook(price,side) != null)
                .map(e->e.getKey())
                .collect(Collectors.toList());
    }
    @Override
    public void clearAll() {
        super.clearAll();
        singleOrderBooks.clear();
    }

    public void clear(Source source) {
        SingleOrderBook singleOrderBook = singleOrderBooks.remove(source);
        if(singleOrderBook == null) {
            return;
        }
        clear(singleOrderBook.stream(Side.BUY).iterator());
        clear(singleOrderBook.stream(Side.SELL).iterator());
    }

    private void clear(Iterator<Map.Entry<BigDecimal,Depth>> iterator) {
        while (iterator.hasNext()) {
            Map.Entry<BigDecimal,Depth> entry = iterator.next();
            Depth depth = entry.getValue();
            BigDecimal sourceVolume = depth.getVolume();
            depth.sub(sourceVolume);
            if(BigDecimal.ZERO.compareTo(depth.getVolume()) == 0) {
                iterator.remove();
            }
        }
    }

    public void setEventId(Source source,long eventId) {
        SingleOrderBook singleOrderBook = singleOrderBooks.get(source);
        singleOrderBook.eventId = eventId;
    }
    public long getEventId(Source source) {
        return singleOrderBooks.get(source).eventId;
    }

    /**
     * @return 更新是否是有效更新
     * 当更新的价格所在的位置超出了map的最大档位时，认为更新无效，其余情况均认为成功
     */
    public boolean update(Side side, BigDecimal price, BigDecimal volume, Source source) {
        ConcurrentSkipListMap<BigDecimal,Depth> sumMap = getMap(side);
        Depth sumDepth = sumMap.get(price);
        if(sumDepth == null) {
            sumDepth = sumMap.computeIfAbsent(price, p->Depth.builder().side(side).price(price).build());
        }
        BigDecimal old = singleOrderBooks.get(source).update(side,price,volume);

        sumDepth.sub(old).add(volume);
        if(BigDecimal.ZERO.compareTo(sumDepth.getVolume()) == 0) {
            sumMap.remove(price);
            return true;
        }

        ConcurrentSkipListMap<BigDecimal,Depth> singleMap = singleOrderBooks.get(source).getMap(side);
        if(singleMap.size() > makerConfig.getMemOrderBookLimit()) {
            // 在多数据源的情况下，这可能导致最后的一些档位的volume少于实际多数据源的总和
            BigDecimal lastPrice = singleMap.lastKey();
            Depth depth = singleMap.remove(lastPrice);
            sumDepth = sumMap.get(lastPrice);
            sumDepth.sub(depth.getVolume());
            if(BigDecimal.ZERO.compareTo(sumDepth.getVolume()) == 0) {
                sumMap.remove(lastPrice);
            }
            return !side.isAfter(singleMap.lastKey(),price);
        }
        return true;
    }

    private class SingleOrderBook extends AbstractOrderBook<Depth> {
        private long eventId;

        public BigDecimal update(Side side, BigDecimal price, BigDecimal volume) {
            ConcurrentSkipListMap<BigDecimal,Depth> singleMap = getMap(side);
            Depth singleDepth = singleMap.get(price);
            if(singleDepth == null && BigDecimal.ZERO.compareTo(volume) == 0) {
                // 推送时会出现第一次出现的深度为0的情况，可能是在中间版本出现过，这种情况不需要处理
                return BigDecimal.ZERO;
            }
            if(singleDepth == null) {
                singleDepth = singleMap.computeIfAbsent(price, p->Depth.builder().side(side).price(price).build());
            }
            BigDecimal old = singleDepth.getVolume();
            if(BigDecimal.ZERO.compareTo(volume) == 0) {
                singleMap.remove(price);
            }
            singleDepth.update(volume);
            return old;
        }
    }

    public static void main(String[] args) {
        SubscribedOrderBook book = new SubscribedOrderBook(null);
        book.update(Side.SELL,BigDecimal.valueOf(10L),BigDecimal.ONE,Source.Binance);
        book.update(Side.SELL,BigDecimal.valueOf(9L),BigDecimal.ONE,Source.Binance);
        book.update(Side.SELL,BigDecimal.valueOf(8L),BigDecimal.ONE,Source.Binance);
        book.update(Side.SELL,BigDecimal.valueOf(7L),BigDecimal.ONE,Source.Binance);
        book.update(Side.SELL,BigDecimal.valueOf(6L),BigDecimal.ONE,Source.Binance);
        book.update(Side.BUY,BigDecimal.valueOf(5L),BigDecimal.ONE,Source.Binance);
        book.update(Side.BUY,BigDecimal.valueOf(4L),BigDecimal.ONE,Source.Binance);
        book.update(Side.BUY,BigDecimal.valueOf(3L),BigDecimal.ONE,Source.Binance);
        book.update(Side.BUY,BigDecimal.valueOf(2L),BigDecimal.ONE,Source.Binance);
        book.update(Side.BUY,BigDecimal.valueOf(1L),BigDecimal.ONE,Source.Binance);
        System.out.println(book);
        System.out.println(book.getBestBuy());
        System.out.println(book.getBestSell());
        System.out.println(book.getFartherBooks(BigDecimal.valueOf(2L),5,Side.BUY));
        System.out.println(book.getFartherBooks(BigDecimal.valueOf(7L),5,Side.SELL));
        System.out.println(book.getNearerBooks(BigDecimal.valueOf(2L),Side.BUY));
        System.out.println(book.getNearerBooks(BigDecimal.valueOf(7L),Side.SELL));
    }

}

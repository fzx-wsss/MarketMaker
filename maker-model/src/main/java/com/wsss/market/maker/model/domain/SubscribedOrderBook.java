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
import java.util.concurrent.ConcurrentSkipListMap;

@Slf4j
@Getter
@Setter
@ToString
@Component
@Scope("prototype")
public class SubscribedOrderBook extends AbstractOrderBook<Depth> {

    @Resource
    private MakerConfig makerConfig;
    private long eventId;
    public SubscribedOrderBook(SymbolInfo symbolInfo) {
        this.symbolInfo = symbolInfo;
    }

    public void clear() {
        buys.clear();
        sells.clear();
        eventId = 0;
    }

    /**
     * @return 更新是否是有效更新
     * 当更新的价格所在的位置超出了map的最大档位时，认为更新无效，其余情况均认为成功
     */
    public boolean update(Side side, BigDecimal price, BigDecimal volume, Source source) {
        ConcurrentSkipListMap<BigDecimal,Depth> map = side == Side.BUY ? buys : sells;
        Depth depth = map.get(price);
        if(depth == null && BigDecimal.ZERO.compareTo(volume) == 0) {
            // 推送时会出现第一次出现的深度为0的情况，可能是在中间版本出现过，这种情况不需要处理
            return true;
        }
        if(depth == null) {
            depth = map.computeIfAbsent(price, p->Depth.builder().side(side).price(price).build());
        }
        BigDecimal sum = depth.update(source,volume);
        if(BigDecimal.ZERO.compareTo(sum) == 0) {
            map.remove(price);
        }
        if(map.size() > makerConfig.getMemOrderBookLimit()) {
            // 在多数据源的情况下，这可能导致最后的一些档位的volume少于实际多数据源的总和
            map.remove(map.lastKey());
            return !side.isAfter(map.lastKey(),price);
        }
        return true;
    }

    public static void main(String[] args) {
        SubscribedOrderBook book = new SubscribedOrderBook(null);
        book.update(Side.SELL,BigDecimal.valueOf(10L),BigDecimal.ONE,Source.BIAN);
        book.update(Side.SELL,BigDecimal.valueOf(9L),BigDecimal.ONE,Source.BIAN);
        book.update(Side.SELL,BigDecimal.valueOf(8L),BigDecimal.ONE,Source.BIAN);
        book.update(Side.SELL,BigDecimal.valueOf(7L),BigDecimal.ONE,Source.BIAN);
        book.update(Side.SELL,BigDecimal.valueOf(6L),BigDecimal.ONE,Source.BIAN);
        book.update(Side.BUY,BigDecimal.valueOf(5L),BigDecimal.ONE,Source.BIAN);
        book.update(Side.BUY,BigDecimal.valueOf(4L),BigDecimal.ONE,Source.BIAN);
        book.update(Side.BUY,BigDecimal.valueOf(3L),BigDecimal.ONE,Source.BIAN);
        book.update(Side.BUY,BigDecimal.valueOf(2L),BigDecimal.ONE,Source.BIAN);
        book.update(Side.BUY,BigDecimal.valueOf(1L),BigDecimal.ONE,Source.BIAN);
        System.out.println(book);
        System.out.println(book.getBestBuy());
        System.out.println(book.getBestSell());
        System.out.println(book.getFartherBooks(BigDecimal.valueOf(2L),5,Side.BUY));
        System.out.println(book.getFartherBooks(BigDecimal.valueOf(7L),5,Side.SELL));
        System.out.println(book.getNearerBooks(BigDecimal.valueOf(2L),Side.BUY));
        System.out.println(book.getNearerBooks(BigDecimal.valueOf(7L),Side.SELL));
    }

}

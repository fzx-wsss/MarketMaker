package com.wsss.market.maker.model.depth.design;

import com.superatomfin.share.tools.other.TimeSieve;
import com.wsss.market.maker.model.config.MakerConfig;
import com.wsss.market.maker.model.domain.*;
import com.wsss.market.maker.model.utils.BigDecimalUtils;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractDepthDesignPolicy implements DepthDesignPolicy {
    protected SymbolInfo symbolInfo;
    @Resource
    protected MakerConfig makerConfig;

    protected TimeSieve timeSieve = TimeSieve.builder().time(System.currentTimeMillis()).build();

    protected abstract SubscribedOrderBook getSubscribedOrderBook();

    protected void followPlaceOrCancelOrder(MakerContext makerContext) {
        OwnerOrderBook ownerOrderBook = symbolInfo.getOwnerOrderBook();
        int maxLevel = makerConfig.getMaxLevel(symbolInfo.getSymbolAo());
        int priceScale = symbolInfo.getSymbolAo().getShowPriceScale();
        Map<BigDecimal, List<Depth>> followBuyDepths = getSubscribedOrderBook().getFartherBooks(makerContext.getMakeBestBuy(), maxLevel,Side.BUY)
                .stream().collect(Collectors.groupingBy(
                        d -> d.getPrice().setScale(priceScale, BigDecimal.ROUND_DOWN)
                ));
        Map<BigDecimal, List<Depth>> followSellDepths = getSubscribedOrderBook().getFartherBooks(makerContext.getMakeBestSell(), maxLevel,Side.SELL)
                .stream().collect(Collectors.groupingBy(
                        d -> d.getPrice().setScale(priceScale, BigDecimal.ROUND_DOWN)
                ));
        Set<BigDecimal> ownerBuyPrices = ownerOrderBook.getBuyPrices();
        Set<BigDecimal> ownerSellPrices = ownerOrderBook.getSellPrices();

        addPlaceOrders(followBuyDepths, ownerBuyPrices, makerContext, Side.BUY);
        addPlaceOrders(followSellDepths, ownerSellPrices, makerContext, Side.SELL);

        addRemoveOrders(ownerBuyPrices, followBuyDepths, makerContext, ownerOrderBook, Side.BUY);
        addRemoveOrders(ownerSellPrices, followSellDepths, makerContext, ownerOrderBook, Side.SELL);

        randomReplaceOrder(ownerBuyPrices,ownerSellPrices,followBuyDepths,followSellDepths,makerContext);
    }

    protected void randomReplaceOrder(Set<BigDecimal> ownerBuyPrices,Set<BigDecimal> ownerSellPrices
            , Map<BigDecimal, List<Depth>> followBuyDepths,Map<BigDecimal, List<Depth>> followSellDepths, MakerContext makerContext) {
        int rate = makerConfig.getReplaceRate();
        if(rate < 0) {
            return;
        }
        timeSieve.moreThanExec(()-> {
            OwnerOrderBook ownerOrderBook = symbolInfo.getOwnerOrderBook();
            int volScale = symbolInfo.getSymbolAo().getShowVolumeScale();
            double volRandomNum = makerConfig.getVolRandomNum(symbolInfo.getSymbolAo());
            double volMultipleNum = makerConfig.getVolMultipleNum(symbolInfo.getSymbolAo());
            ownerBuyPrices.forEach(p -> {
                if(ThreadLocalRandom.current().nextInt(1000) >= rate) {
                    return;
                }
                List<Depth> depths = followBuyDepths.get(p);
                if (depths == null || depths.isEmpty()) {
                    return;
                }

                makerContext.addRemoveOrder(ownerOrderBook.getBook(p, Side.BUY));
                BigDecimal realVol = calVol(depths,volScale,volRandomNum,volMultipleNum);
                if(realVol != null) {
                    makerContext.addPlaceOrder(new Order(p, realVol, Side.BUY));
                }
            });

            ownerSellPrices.forEach(p -> {
                if(ThreadLocalRandom.current().nextInt(100) >= rate) {
                    return;
                }
                List<Depth> depths = followSellDepths.get(p);
                if (depths == null || depths.isEmpty()) {
                    return;
                }

                makerContext.addRemoveOrder(ownerOrderBook.getBook(p, Side.SELL));
                BigDecimal realVol = calVol(depths,volScale,volRandomNum,volMultipleNum);
                if(realVol != null) {
                    makerContext.addPlaceOrder(new Order(p, realVol, Side.SELL));
                }
            });
        }, TimeUnit.SECONDS.toMillis(makerConfig.getReplaceInterval()));
    }

    protected void addPlaceOrders(Map<BigDecimal, List<Depth>> followDepths, Set<BigDecimal> ownerPrices, MakerContext makerContext, Side side) {
        int volScale = symbolInfo.getSymbolAo().getShowVolumeScale();
        double volRandomNum = makerConfig.getVolRandomNum(symbolInfo.getSymbolAo());
        double volMultipleNum = makerConfig.getVolMultipleNum(symbolInfo.getSymbolAo());
        followDepths.forEach((k, l) -> {
            if (ownerPrices.contains(k)) {
                return;
            }
            BigDecimal realVol = calVol(l,volScale,volRandomNum,volMultipleNum);
            if(realVol != null) {
                makerContext.addPlaceOrder(new Order(k, realVol, side));
            }
        });
    }

    protected BigDecimal calVol(List<Depth> depths,int volScale,double volRandomNum,double volMultipleNum) {
        // 不需要精确，所以可以使用double
        double oriVol = depths.stream().mapToDouble(d -> d.getVolume().doubleValue()).sum();
        BigDecimal realVol = BigDecimal.valueOf(oriVol * volMultipleNum + (ThreadLocalRandom.current().nextBoolean() ? oriVol * volRandomNum : -oriVol * volRandomNum))
                .setScale(volScale, BigDecimal.ROUND_DOWN);
        if (realVol.compareTo(BigDecimal.ZERO) <= 0) {
            // 数量过小则丢弃
            return null;
        }
        return realVol;
    }

    protected void addRemoveOrders(Set<BigDecimal> ownerPrices, Map<BigDecimal, List<Depth>> followDepths, MakerContext makerContext, OwnerOrderBook ownerOrderBook, Side side) {
        ownerPrices.forEach(p -> {
            if (followDepths.containsKey(p)) {
                return;
            }
            makerContext.addRemoveOrder(ownerOrderBook.getBook(p, side));
        });
    }

    protected void avoidOrEatUserOrder(MakerContext makerContext) {
        if (!makerConfig.isUserBulkOrderSwitch()) {
            return;
        }
        UserBBO userBBO = symbolInfo.getUserBBO();
        if (userBBO == null) {
            return;
        }
        String symbolName = symbolInfo.getSymbol();
        BigDecimal oneStep = BigDecimal.valueOf(1, symbolInfo.getSymbolAo().getShowPriceScale());
        if (userBBO.getUserBuyPrice() != null) {
            BigDecimal userBestBid = userBBO.getUserBuyPrice();
            BigDecimal userBestBidVolume = userBBO.getUserBuyVolume();
            // price * volume * size factor
            BigDecimal amount = userBestBid.multiply(userBestBidVolume);
            BigDecimal userOrderLimitAmount = makerConfig.getUserBulkOrderLimit(symbolInfo.getSymbolAo());
            log.warn("{}-userBook:{},{} limit: {}", symbolName, userBBO.getUserBuyPrice(), userBBO.getUserBuyVolume(), userOrderLimitAmount);
            // best ask低于用户买单，避让
            if (makerContext.getSpreadBestSell().compareTo(userBestBid) <= 0 && amount.compareTo(userOrderLimitAmount) >= 0) {
                log.info("{} userBulk {} order:{}@{}({})", symbolName, Side.BUY, userBestBidVolume, userBestBid, amount);
                makerContext.setMakeBestSell(userBestBid.add(oneStep));
            }

            int userOrderStrike = makerConfig.getUserBulkOrderStrikeOffBps(symbolInfo.getSymbolAo());
            // 10000 - 摆盘最佳买价 / (避让用户买单价格 * 10000) = 用户买单低于『市价』的万分比
            if (BigDecimalUtils.WAN.subtract(makerContext.getRealBestBuy().divide(userBestBid).multiply(BigDecimalUtils.WAN)).intValue() > userOrderStrike) {
                log.info("{} userStrike {} order realBid:{} userBestBid:{}(amount:{})", symbolName, Side.BUY, makerContext.getRealBestBuy(), userBestBid, amount);
                makerContext.addPlaceOrder(new Order(userBestBid, userBestBidVolume, Side.SELL));
            }
        }

        if (userBBO.getUserSellPrice() != null) {
            BigDecimal userBestAsk = userBBO.getUserSellPrice();
            BigDecimal userBestAskVolume = userBBO.getUserSellVolume();
            // price * volume * size factor
            BigDecimal amount = userBestAsk.multiply(userBestAskVolume);
            // 避让
            BigDecimal userOrderLimitAmount = makerConfig.getUserBulkOrderLimit(symbolInfo.getSymbolAo());
            log.warn("{}-userBook:{},{} limit: {}", symbolName, userBBO.getUserBuyPrice(), userBBO.getUserBuyVolume(), userOrderLimitAmount);
            if (makerContext.getMakeBestBuy().compareTo(userBestAsk) >= 0 && amount.compareTo(userOrderLimitAmount) >= 0) {
                log.warn("{} userBulk {} order:{}@{}({})", symbolName, Side.SELL, userBestAskVolume, userBestAsk, amount);
                makerContext.setMakeBestBuy(userBestAsk.subtract(oneStep));
            }
            int userOrderStrike = makerConfig.getUserBulkOrderStrikeOffBps(symbolInfo.getSymbolAo());
            // realBestSell / userBestAsk * 10000 - 10000 > userOrderStrike
            if (makerContext.getRealBestSell().divide(userBestAsk).multiply(BigDecimalUtils.WAN).subtract(BigDecimalUtils.WAN).intValue() > userOrderStrike) {
                log.warn("{} userStrike {} order realOffer:{} userBestAsk:{}(amount:{})", symbolName, Side.SELL, makerContext.getRealBestSell(), userBestAsk, amount);
                makerContext.addPlaceOrder(new Order(userBestAsk, userBestAskVolume, Side.BUY));
            }
        }
    }

    protected MakerContext getMakerContext() {
        int spreadFloat = makerConfig.getSpreadLowLimitMillesimal(symbolInfo.getSymbolAo());
        int oneSideGetOutBuyBps = makerConfig.getOneSideGetOutBps(symbolInfo.getSymbolAo(), Side.BUY);
        int oneSideGetOutSellBps = makerConfig.getOneSideGetOutBps(symbolInfo.getSymbolAo(), Side.SELL);
        long floatDownward = oneSideGetOutBuyBps > 0 ? 10000 - oneSideGetOutBuyBps : 10000 - spreadFloat / 2;
        long floatUpward = oneSideGetOutSellBps > 0 ? 10000 + oneSideGetOutSellBps : 10000 + spreadFloat / 2;
        BigDecimal bestBuy = getSubscribedOrderBook().getBestBuy();
        BigDecimal bestSell = getSubscribedOrderBook().getBestSell();

        if(bestBuy.compareTo(bestSell) >= 0) {
            // 盘口交叉了的话就将价格反过来
            BigDecimal oneStep = BigDecimal.valueOf(1, symbolInfo.getSymbolAo().getShowPriceScale());
            BigDecimal temp = bestBuy;
            bestBuy = bestSell.subtract(oneStep);
            bestSell = temp.add(oneStep);
        }
        if (bestSell == null || bestBuy == null) {
            log.warn("bbo is empty! symbol:{}, bestBuy:{}, bestSell:{}", symbolInfo.getSymbol(), bestBuy, bestSell);
            return null;
        }
        return MakerContext.builder()
                .realBestBuy(bestBuy)
                .realBestSell(bestSell)
                .spreadBestBuy(bestBuy.multiply(BigDecimalUtils.convert(floatDownward)).divide(BigDecimalUtils.WAN))
                .spreadBestSell(bestSell.multiply(BigDecimalUtils.convert(floatUpward)).divide(BigDecimalUtils.WAN))
                .build();
    }
}

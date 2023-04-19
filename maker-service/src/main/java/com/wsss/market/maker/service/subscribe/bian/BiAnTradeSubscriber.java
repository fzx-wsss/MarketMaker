package com.wsss.market.maker.service.subscribe.bian;

import com.wsss.market.maker.model.config.BiAnConfig;
import com.wsss.market.maker.model.domain.CacheMap;
import com.wsss.market.maker.model.domain.SymbolInfo;
import com.wsss.market.maker.model.utils.Perf;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.JsonNode;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Map;

@Slf4j
@Component
@Scope("prototype")
public class BiAnTradeSubscriber extends BiAnAbstractSubscriber {
    private static String suffix = "@aggTrade";
    private static String SUB = "{\"method\": \"SUBSCRIBE\",\"params\":[\"%s"+suffix+"\"],\"id\": 1}";
    private static Map<String,String> symbolConvertMap = new CacheMap<>(s->s.substring(0,s.length()-suffix.length()));

    @Resource
    private BiAnConfig biAnConfig;

    @Override
    protected String getSteamUrl() {
        return biAnConfig.getBinanceSteamUrl();
    }

    @Override
    protected BiAnSubMsg buildRegisterMsg(Collection<String> symbols) {
        BiAnSubMsg msg = new BiAnSubMsg();
        msg.setMethod(BiAnSubMsg.SUBSCRIBE);
        symbols.forEach(s -> {
            msg.addParams(s+suffix);
        });
        return msg;
    }

    @Override
    protected BiAnSubMsg buildRemoveMsg(Collection<String> symbols) {
        BiAnSubMsg msg = new BiAnSubMsg();
        msg.setMethod(BiAnSubMsg.UNSUBSCRIBE);
        symbols.forEach(s -> {
            msg.addParams(s+suffix);
        });
        return msg;
    }

    @Override
    protected void notifyProcessThread(SymbolInfo symbolInfo, String childSymbolName, JsonNode data) {
        Perf.count("bi_an_receive_trade_msg",symbolInfo);
        if(symbolInfo.isDebugLog()) {
            log.info("bi_an_receive_trade_msg:{}",data);
        }
        // 按主币对划分执行线程
        BiAnTradeListenTask task = new BiAnTradeListenTask(symbolInfo,data);
        markerMakerThreadPool.getTradeProcessThread(symbolInfo.getSymbol()).offer(task);
    }

    @Override
    protected String convertSymbolName(String name) {
        return symbolConvertMap.get(name);
    }
}

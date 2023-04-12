package com.wsss.market.maker.depth.subscribe.bian;

import com.cmcm.finance.common.util.JsonUtil;
import com.wsss.market.maker.config.BiAnConfig;
import com.wsss.market.maker.domain.CacheMap;
import com.wsss.market.maker.domain.SymbolInfo;
import com.wsss.market.maker.utils.JacksonMapper;
import org.codehaus.jackson.JsonNode;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Map;

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
    protected void doRegister(List<String> symbols) {
        BiAnSubMsg msg = new BiAnSubMsg();
        symbols.forEach(s -> {
            msg.addParams(s+suffix);
        });
        try {
            String sendMsg = JsonUtil.encode(msg);
            wsClient.send(sendMsg);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void notifyProcessThread(SymbolInfo symbolInfo, String childSymbolName, JsonNode data) {
        // 按主币对划分执行线程
        BiAnTradeListenTask task = new BiAnTradeListenTask(symbolInfo,data);
        markerMakerThreadPool.getTradeProcessThread(symbolInfo.getSymbol()).offer(task);
    }

    @Override
    protected String convertSymbolName(String name) {
        return symbolConvertMap.get(name);
    }
}

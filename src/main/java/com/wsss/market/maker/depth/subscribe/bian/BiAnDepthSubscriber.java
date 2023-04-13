package com.wsss.market.maker.depth.subscribe.bian;

import com.cmcm.finance.common.util.JsonUtil;
import com.superatomfin.framework.monitor.Monitor;
import com.wsss.market.maker.config.BiAnConfig;
import com.wsss.market.maker.domain.CacheMap;
import com.wsss.market.maker.domain.SymbolInfo;
import com.wsss.market.maker.utils.JacksonMapper;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.JsonNode;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
@Slf4j
@Component
@Scope("prototype")
public class BiAnDepthSubscriber extends BiAnAbstractSubscriber {
    private static String suffix = "@depth@100ms";
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
            log.info("bi an sendMsg:{}",sendMsg);
            wsClient.send(sendMsg);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    protected void notifyProcessThread(SymbolInfo symbolInfo, String childSymbolName, JsonNode data) {
        Monitor.counter("bi_an_receive_depth_msg").end();
        if(symbolInfo.isDebugLog()) {
            log.info("bi_an_receive_depth_msg:{}",data);
        }
        // 按子币对创建任务，同步订单簿
        BiAnDepthListenTask task = new BiAnDepthListenTask(symbolInfo,data,symbolInfo.getChildSubscribedOrderBook(childSymbolName));
        symbolInfo.putDepthListenTask(childSymbolName,task);
        // 按主币对划分执行线程
        markerMakerThreadPool.getDepthProcessThread(symbolInfo.getSymbol()).offer(symbolInfo.getSymbol());
    }

    @Override
    protected String convertSymbolName(String name) {
        return symbolConvertMap.get(name);
    }

}

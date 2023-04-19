package com.wsss.market.maker.service.subscribe.bian;

import com.superatomfin.framework.monitor.Monitor;
import com.wsss.market.maker.model.config.BiAnConfig;
import com.wsss.market.maker.model.domain.CacheMap;
import com.wsss.market.maker.model.domain.SymbolInfo;
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

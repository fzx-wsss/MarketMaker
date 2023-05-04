package com.wsss.market.maker.service.subscribe.bian;

import com.wsss.market.maker.model.domain.CacheMap;
import com.wsss.market.maker.model.domain.SymbolInfo;
import com.wsss.market.maker.model.utils.Perf;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.JsonNode;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@Scope("prototype")
public class BiAnDepthSubscriber extends BiAnAbstractSubscriber {
    private static String suffix = "@depth@100ms";
    private static Map<String,String> symbolConvertMap = new CacheMap<>(s->s.substring(0,s.length()-suffix.length()));

    @Override
    protected void notifyProcessThread(SymbolInfo symbolInfo, String childSymbolName, JsonNode data) {
        Perf.count("bi_an_receive_depth_msg",symbolInfo);
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
    protected String getSuffix() {
        return suffix;
    }

    @Override
    protected String convertSymbolName(String name) {
        return symbolConvertMap.get(name);
    }

}

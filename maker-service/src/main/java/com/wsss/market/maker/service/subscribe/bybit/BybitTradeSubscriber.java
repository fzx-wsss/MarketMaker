package com.wsss.market.maker.service.subscribe.bybit;

import com.fasterxml.jackson.databind.JsonNode;
import com.superatomfin.share.tools.utils.JsonUtils;
import com.wsss.market.maker.model.domain.Source;
import com.wsss.market.maker.model.domain.SymbolInfo;
import com.wsss.market.maker.model.utils.Perf;
import com.wsss.market.maker.model.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@Scope("prototype")
@Slf4j
public class BybitTradeSubscriber extends BybitAbstractSubscriber {

    @Override
    protected void doRegisterMsg(Collection<String> symbols) {
        BybitSubMsg msg = BybitSubMsg.buildSubscribe();
        symbols.forEach(s -> {
            msg.addArg("publicTrade." + s.toUpperCase());
        });

        String sendMsg = JsonUtils.encode(msg);
        super.sendMsg(sendMsg);
    }

    @Override
    protected void doRemoveMsg(Collection<String> symbols) {
        BybitSubMsg msg = BybitSubMsg.buildUnsubscribe();
        symbols.forEach(s -> {
            msg.addArg("publicTrade." + s.toUpperCase());
        });

        String sendMsg = JsonUtils.encode(msg);
        super.sendMsg(sendMsg);
    }

    @Override
    public void receive(String msg) {
        JsonNode root = JsonUtils.decode(msg);
        if (!root.has("data")) {
            log.warn("receive unknown msg: {}",root);
            return;
        }
        String topic = root.get("topic").asText();
        if (!topic.startsWith("publicTrade.")) {
            log.warn("unknown msg:{}",root);
            return;
        }
        updateLastReceiveTime();
        String childSymbolName = StringUtils.toLowerSymbol(root.get("data").get(0).get("s").asText());
        if(dataCenter.getMappingSymbolInfo(Source.Bybit,childSymbolName).isEmpty()) {
            log.warn("childSymbolName:{} mapping symbol info is empty",childSymbolName);
        }
        dataCenter.getMappingSymbolInfo(Source.Bybit,childSymbolName).forEach(symbolInfo -> {
            notifyProcessThread(symbolInfo,childSymbolName,root);
        });
    }

    protected void notifyProcessThread(SymbolInfo symbolInfo, String childSymbolName, JsonNode data) {
        Perf.count("bybit_receive_trade_msg", symbolInfo);
        if (symbolInfo.isDebugLog()) {
            log.info("bybit_receive_trade_msg:{}", data);
        }
        // 按主币对划分执行线程
        BybitTradeListenTask task = new BybitTradeListenTask(symbolInfo, data, childSymbolName);
        markerMakerThreadPool.getTradeProcessThread(symbolInfo.getSymbol()).offer(task);
    }

}
package com.wsss.market.maker.inner.supplier.binance;


import com.superatomfin.share.tools.utils.JsonUtils;
import com.wsss.market.maker.inner.api.receive.AbstractSubscriber;
import com.wsss.market.maker.model.domain.Source;
import com.wsss.market.maker.model.domain.SymbolInfo;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.JsonNode;

import java.util.Collection;

@Slf4j
public abstract class BiAnAbstractSubscriber extends AbstractSubscriber {

    @Override
    protected String getSteamUrl() {
        return sourceConfig.getBinanceSteamUrl();
    }

    @Override
    protected void doRegisterMsg(Collection<String> symbols) {
        BiAnSubMsg msg = new BiAnSubMsg();
        msg.setMethod(BiAnSubMsg.SUBSCRIBE);
        symbols.forEach(s -> {
            msg.addParams(s + getSuffix());
        });
        String sendMsg = JsonUtils.encode(msg);
        super.sendMsg(sendMsg);
    }

    @Override
    protected void doRemoveMsg(Collection<String> symbols) {
        BiAnSubMsg msg = new BiAnSubMsg();
        msg.setMethod(BiAnSubMsg.UNSUBSCRIBE);
        symbols.forEach(s -> {
            msg.addParams(s + getSuffix());
        });
        String sendMsg = JsonUtils.encode(msg);
        super.sendMsg(sendMsg);
    }

    protected abstract void notifyProcessThread(SymbolInfo symbolInfo, String childSymbolName, JsonNode data);

    protected abstract String getSuffix();
    protected abstract String convertSymbolName(String name);

    @Override
    public void receive(String msg) {
        try {
            JsonNode root = com.wsss.market.maker.utils.JacksonMapper.getInstance().readTree(msg);
            if (!root.has("stream")) {
                log.warn("receive unknown msg: {}", root);
                return;
            }
            updateLastReceiveTime();
            String childSymbolName = convertSymbolName(root.get("stream").asText());
            JsonNode data = root.get("data");
            if (dataCenter.getMappingSymbolInfo(Source.Binance, childSymbolName).isEmpty()) {
                log.warn("childSymbolName:{}", childSymbolName);
            }
            dataCenter.getMappingSymbolInfo(Source.Binance, childSymbolName).forEach(symbolInfo -> {
                notifyProcessThread(symbolInfo, childSymbolName, data);
            });
        } catch (Exception e) {
            log.error("BiAnWSListener receive error:{}", msg);
            throw new RuntimeException(e);
        }
    }
}

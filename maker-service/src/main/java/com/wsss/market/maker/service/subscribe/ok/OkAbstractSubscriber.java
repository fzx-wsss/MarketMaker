package com.wsss.market.maker.service.subscribe.ok;

import com.superatomfin.share.tools.other.Sleep;
import com.wsss.market.maker.model.domain.CacheMap;
import com.wsss.market.maker.model.domain.Source;
import com.wsss.market.maker.model.domain.SymbolInfo;
import com.wsss.market.maker.service.subscribe.AbstractSubscriber;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.JsonNode;

import java.util.Collection;

@Slf4j
public abstract class OkAbstractSubscriber extends AbstractSubscriber {

    protected CacheMap<String,String> symbolName = new CacheMap<>(k->{
        if(k.endsWith("usdt") || k.endsWith("usdc") || k.endsWith("busd")|| k.endsWith("tusd")) {
            return k.substring(0,k.length()-4).toUpperCase() + "-" + k.substring(k.length()-4).toUpperCase();
        }
        if(k.endsWith("xrp") || k.endsWith("btc") || k.endsWith("eth")|| k.endsWith("bnb")) {
            return k.substring(0,k.length()-3).toUpperCase() + "-" + k.substring(k.length()-3).toUpperCase();
        }
        throw new UnsupportedOperationException(k);
    });

    private CacheMap<String,String> convertSymbolName = new CacheMap<>(k->k.replaceAll("-","").toLowerCase());

    @Override
    protected void reRegister(Collection<String> symbols) {
        super.reRegister(symbols);
        // ok每秒只能建立一个连接
        Sleep.sleepSeconds(1);
    }

    @Override
    protected String getSteamUrl() {
        return sourceConfig.getOkSteamUrl();
    }

    @Override
    public void receive(String msg) {
        try {
            JsonNode root = com.wsss.market.maker.utils.JacksonMapper.getInstance().readTree(msg);
            if (!root.has("arg")) {
                log.warn("receive unknown msg: {}",root);
                return;
            }
            updateLastReceiveTime();
            String childSymbolName = convertSymbolName.get(root.get("arg").get("instId").asText());
            if(dataCenter.getMappingSymbolInfo(Source.Okex,childSymbolName).isEmpty()) {
                log.warn("childSymbolName:{} mapping symbol info is empty",childSymbolName);
            }
            dataCenter.getMappingSymbolInfo(Source.Okex,childSymbolName).forEach(symbolInfo -> {
                notifyProcessThread(symbolInfo,childSymbolName,root);
            });
        } catch (Exception e) {
            log.error("OkWSListener receive error:{}",msg);
            throw new RuntimeException(e);
        }
    }

    protected abstract void notifyProcessThread(SymbolInfo symbolInfo, String childSymbolName, JsonNode data);
}

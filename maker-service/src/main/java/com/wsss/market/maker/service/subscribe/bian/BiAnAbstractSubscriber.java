package com.wsss.market.maker.service.subscribe.bian;

import com.cmcm.finance.common.util.JsonUtil;
import com.wsss.market.maker.model.domain.SymbolInfo;
import com.wsss.market.maker.model.ws.WSClient;
import com.wsss.market.maker.model.ws.WSListener;
import com.wsss.market.maker.model.ws.netty.NettyWSClient;
import com.wsss.market.maker.service.center.DataCenter;
import com.wsss.market.maker.service.thread.pool.MarkerMakerThreadPool;
import com.wsss.market.maker.utils.JacksonMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.JsonNode;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

@Slf4j
public abstract class BiAnAbstractSubscriber {

    @Resource
    protected DataCenter dataCenter;
    @Resource
    protected MarkerMakerThreadPool markerMakerThreadPool;

    protected WSClient wsClient;
    @Getter
    protected Set<String> subscribedSymbol = new HashSet<>();

    @PostConstruct
    public void init() {
        try {
            wsClient = NettyWSClient.builder().websocketURI(new URI(getSteamUrl())).wsListener(getWSListener()).build();
            wsClient.connect();
            log.info("subscriber url:{}",getSteamUrl());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract String getSteamUrl();

    public boolean register(Collection<String> symbols) {
        if(symbols.isEmpty()) {
            return true;
        }
        try {
            BiAnSubMsg msg = doRegisterMsg(symbols);
            sendMsg(msg);
        } finally {
            subscribedSymbol.addAll(symbols);
        }
        return true;
    }

    public boolean remove(Set<String> symbols) {
        if(symbols.isEmpty()) {
            return true;
        }
        try {
            BiAnSubMsg msg = doRemoveMsg(symbols);
            sendMsg(msg);
        } finally {
            subscribedSymbol.removeAll(symbols);
        }
        return true;
    }

    protected void sendMsg(BiAnSubMsg msg) {
        try {
            String sendMsg = JsonUtil.encode(msg);
            log.info("bi an sendMsg:{}",sendMsg);
            wsClient.send(sendMsg);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected WSListener getWSListener() {
        return new BiAnWSListener();
    }

    protected abstract BiAnSubMsg doRegisterMsg(Collection<String> symbols);
    protected abstract BiAnSubMsg doRemoveMsg(Collection<String> symbols);


    protected abstract void notifyProcessThread(SymbolInfo symbolInfo, String childSymbolName, JsonNode data);

    protected abstract String convertSymbolName(String name);

    @Getter
    class BiAnSubMsg {
        public final static String UNSUBSCRIBE = "UNSUBSCRIBE";
        public final static String SUBSCRIBE = "SUBSCRIBE";
        @Setter
        private String method;
        private List<String> params = new ArrayList<>();
        private int id = 1;

        public void addParams(String s) {
            params.add(s);
        }
    }

    class BiAnWSListener implements WSListener {
        @Override
        public void receive(String msg) {
            try {
                JsonNode root = JacksonMapper.getInstance().readTree(msg);
                if (!root.has("stream")) {
                    log.warn("receive unknown msg: {}",root);
                    return;
                }
                String childSymbolName = convertSymbolName(root.get("stream").asText());
                JsonNode data = root.get("data");
                if(dataCenter.getMappingSymbolInfo(childSymbolName).isEmpty()) {
                    log.warn("childSymbolName:{}",childSymbolName);
                }
                dataCenter.getMappingSymbolInfo(childSymbolName).forEach(symbolInfo -> {
                    notifyProcessThread(symbolInfo,childSymbolName,data);
                });
            } catch (Exception e) {
                log.error("BiAnWSListener receive error:{}",msg);
                throw new RuntimeException(e);
            }
        }

        @Override
        public void receive(byte[] msg) {

        }

        @Override
        public void success() {
            register(new ArrayList<>(subscribedSymbol));
        }

        @Override
        public void inactive() {

        }
    }
}

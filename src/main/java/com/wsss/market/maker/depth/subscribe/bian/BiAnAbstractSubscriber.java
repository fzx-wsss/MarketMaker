package com.wsss.market.maker.depth.subscribe.bian;

import com.superatomfin.framework.monitor.Monitor;
import com.wsss.market.maker.center.DataCenter;
import com.wsss.market.maker.depth.thread.MarkerMakerThreadPool;
import com.wsss.market.maker.domain.SymbolInfo;
import com.wsss.market.maker.utils.JacksonMapper;
import com.wsss.market.maker.ws.WSClient;
import com.wsss.market.maker.ws.WSListener;
import com.wsss.market.maker.ws.netty.NettyWSClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.JsonNode;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public abstract class BiAnAbstractSubscriber {

    @Resource
    protected DataCenter dataCenter;
    @Resource
    protected MarkerMakerThreadPool markerMakerThreadPool;

    protected WSClient wsClient;
    @Getter
    protected Set<SymbolInfo> subscribedSymbol = new HashSet<>();

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

    public boolean register(List<SymbolInfo> symbolInfoList) {
        if(symbolInfoList.isEmpty()) {
            return true;
        }
        try {
            List<String> symbols = symbolInfoList.stream().map(s->s.getChildSymbol()).flatMap(list->list.stream()).collect(Collectors.toList());
            doRegister(symbols);
        } finally {
            subscribedSymbol.addAll(symbolInfoList);
        }
        return true;
    }

    protected WSListener getWSListener() {
        return new BiAnWSListener();
    }

    protected abstract void doRegister(List<String> symbols);


    protected abstract void notifyProcessThread(SymbolInfo symbolInfo, String childSymbolName, JsonNode data);

    protected abstract String convertSymbolName(String name);

    @Getter
    class BiAnSubMsg {
        private String method = "SUBSCRIBE";
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

                dataCenter.getMappingSymbolInfo(childSymbolName).forEach(symbolInfo -> {
                    notifyProcessThread(symbolInfo,childSymbolName,data);
                });
            } catch (IOException e) {
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

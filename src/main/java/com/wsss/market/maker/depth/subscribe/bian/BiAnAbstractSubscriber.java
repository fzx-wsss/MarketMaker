package com.wsss.market.maker.depth.subscribe.bian;

import com.wsss.market.maker.center.DataCenter;
import com.wsss.market.maker.depth.thread.MarkerMakerThreadPool;
import com.wsss.market.maker.domain.SymbolInfo;
import com.wsss.market.maker.utils.JacksonMapper;
import com.wsss.market.maker.ws.WSClient;
import com.wsss.market.maker.ws.WSListener;
import com.wsss.market.maker.ws.netty.NettyWSClient;
import lombok.Getter;
import org.codehaus.jackson.JsonNode;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract String getSteamUrl();

    public void connect() {
        wsClient.connect();
    }

    public boolean register(List<String> symbol) {
        try {
            if (!wsClient.isAlive()) {
                connect();
            }
            doRegister(symbol);
        } finally {
            subscribedSymbol.addAll(symbol);
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
    }
}

package com.wsss.market.maker.service.subscribe.bian;

import com.cmcm.finance.common.util.JsonUtil;
import com.wsss.market.maker.model.domain.SymbolInfo;
import com.wsss.market.maker.model.ws.WSListener;
import com.wsss.market.maker.service.subscribe.AbstractSubscriber;
import com.wsss.market.maker.utils.JacksonMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;
import java.util.*;

@Slf4j
public abstract class BiAnAbstractSubscriber extends AbstractSubscriber {

    @Override
    protected void doRegisterMsg(Collection<String> symbols) {
        BiAnSubMsg msg = buildRegisterMsg(symbols);
        sendBiAnMsg(msg);
    }

    @Override
    protected void doRemoveMsg(Collection<String> symbols) {
        BiAnSubMsg msg = buildRemoveMsg(symbols);
        sendBiAnMsg(msg);
    }

    protected void sendBiAnMsg(BiAnSubMsg msg) {
        try {
            String sendMsg = JsonUtil.encode(msg);
            super.sendMsg(sendMsg);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected WSListener getWSListener() {
        return new BiAnWSListener();
    }

    protected abstract BiAnSubMsg buildRemoveMsg(Collection<String> symbols);
    protected abstract BiAnSubMsg buildRegisterMsg(Collection<String> symbols);

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
                updateLastReceiveTime();
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
            reRegister(subscribedSymbol);
        }

        @Override
        public void inactive() {

        }
    }
}

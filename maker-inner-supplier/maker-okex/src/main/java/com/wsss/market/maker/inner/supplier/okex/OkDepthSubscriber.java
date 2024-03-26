package com.wsss.market.maker.inner.supplier.okex;

import com.superatomfin.share.tools.utils.JsonUtils;
import com.wsss.market.maker.model.domain.SymbolInfo;
import com.wsss.market.maker.model.utils.Perf;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.JsonNode;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@Scope("prototype")
@Slf4j
public class OkDepthSubscriber extends OkAbstractSubscriber {

    @Override
    protected void doRegisterMsg(Collection<String> symbols) {
        OkMsg msg = OkMsg.buildSubscribe();
        symbols.forEach(s -> {
            try {
                msg.addArg("books",symbolName.get(s));
            } catch (Exception e) {
                log.error("",e);
            }
        });

        String sendMsg = JsonUtils.encode(msg);
        super.sendMsg(sendMsg);
    }

    @Override
    protected void doRemoveMsg(Collection<String> symbols) {
        OkMsg msg = OkMsg.buildUnsubscribe();
        symbols.forEach(s -> {
            try {
                msg.addArg("books",symbolName.get(s));
            } catch (Exception e) {
                log.error("",e);
            }
        });

        String sendMsg = JsonUtils.encode(msg);
        super.sendMsg(sendMsg);
    }

    protected void notifyProcessThread(SymbolInfo symbolInfo, String childSymbolName, JsonNode data) {
        Perf.count("ok_receive_depth_msg",symbolInfo);
        if(symbolInfo.isDebugLog()) {
            log.info("ok_receive_depth_msg:{}",data);
        }
        // 按子币对创建任务，同步订单簿
        OkDepthListenTask task = new OkDepthListenTask(symbolInfo,data,childSymbolName);
        symbolInfo.putDepthListenTask(childSymbolName,task);
        // 按主币对划分执行线程
        markerMakerThreadPool.offerDepth(symbolInfo.getSymbol(), symbolInfo.getSymbol());
    }


}

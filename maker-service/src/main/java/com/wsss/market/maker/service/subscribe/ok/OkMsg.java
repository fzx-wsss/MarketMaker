package com.wsss.market.maker.service.subscribe.ok;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class OkMsg {
    private String op;
    private List<Arg> args = new ArrayList<>();

    public static OkMsg buildSubscribe() {
        OkMsg okMsg = new OkMsg();
        okMsg.op = "subscribe";
        return okMsg;
    }
    public static OkMsg buildUnsubscribe() {
        OkMsg okMsg = new OkMsg();
        okMsg.op = "unsubscribe";
        return okMsg;
    }

    public void addArg(String channel, String instId) {
        Arg arg = new Arg();
        arg.channel = channel;
        arg.instId = instId;
        args.add(arg);
    }

    @Getter
    private class Arg {
        private String channel;
        private String instId;
    }
}

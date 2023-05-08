package com.wsss.market.maker.service.subscribe.bybit;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class BybitSubMsg {
    private String req_id;
    private String op;
    private List<String> args = new ArrayList<>();

    public static BybitSubMsg buildSubscribe() {
        BybitSubMsg msg = new BybitSubMsg();
        msg.op = "subscribe";
        return msg;
    }
    public static BybitSubMsg buildUnsubscribe() {
        BybitSubMsg msg = new BybitSubMsg();
        msg.op = "unsubscribe";
        return msg;
    }

    public void addArg(String topic) {
        args.add(topic);
    }
}

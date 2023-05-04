package com.wsss.market.maker.service.subscribe.bian;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class BiAnSubMsg {
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
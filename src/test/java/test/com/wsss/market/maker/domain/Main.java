package test.com.wsss.market.maker.domain;

import com.wsss.market.maker.center.DataCenter;

import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        DataCenter.register("xrpusdt");
        TimeUnit.HOURS.sleep(10);
    }
}

package test.com.wsss.market.maker.ws.netty;


import com.wsss.market.maker.ws.WSListener;
import com.wsss.market.maker.ws.netty.NettyWSClient;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws Exception {
        NettyWSClient client = NettyWSClient.builder().websocketURI(new URI("wss://ws.bitrue.com:443/market/ws")).wsListener(new MyWSListener()).build();
        client.connect();
        String putMessage = "{\"event\":\"sub\",\"params\":{\"cb_id\":\"trxusdt\",\"channel\":\"market_btcusdt_simple_depth_step0\",\"top\":20}}";
        client.send(putMessage);
        for(int i=0;i<10000;i++) {
            client.send("{\"pong\":\"123123123\"}");
            TimeUnit.SECONDS.sleep(30);
        }
        TimeUnit.HOURS.sleep(10);
    }

    public static class MyWSListener implements WSListener {
        public void receive(String msg) {
            System.out.println(msg);
        }

        public void receive(byte[] msg) {
        }
    }
}

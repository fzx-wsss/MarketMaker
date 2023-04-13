package com.wsss.market.maker.ws.netty;


import com.wsss.market.maker.ws.WSClient;
import com.wsss.market.maker.ws.WSListener;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NettyWSClient implements WSClient {
    private URI websocketURI;
    private WSListener wsListener;
    private Channel channel;
    private NioEventLoopGroup group = new NioEventLoopGroup();
    @Getter
    private volatile boolean close = true;

    @Builder
    public NettyWSClient(URI websocketURI,WSListener wsListener) {
        this.websocketURI = websocketURI;
        this.wsListener = wsListener;
        if(wsListener == null) {
            this.wsListener = new EmptyWSListener();
        }
    }

    public void connect() {
        close();
        close = false;
        //netty基本操作，启动类
        Bootstrap boot = new Bootstrap();
        boot.option(ChannelOption.SO_KEEPALIVE, true).option(ChannelOption.TCP_NODELAY, true).group(group)
                .handler(new LoggingHandler(LogLevel.DEBUG)).channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        if("wss".equals(websocketURI.getScheme())) {
                            SslContext sslCtx = SslContextBuilder.forClient()
                                    .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
                            pipeline.addLast(sslCtx.newHandler(socketChannel.alloc()));
                        }

                        pipeline.addLast("http-codec",new HttpClientCodec());
                        pipeline.addLast("aggregator", new HttpObjectAggregator(1024 * 1024 * 10));
                        pipeline.addLast(new ChunkedWriteHandler());
                        pipeline.addLast("hookedHandler", new WebSocketClientHandler(websocketURI));
                        pipeline.addLast("textHandler", new WebSocketFrameHandler(wsListener,NettyWSClient.this));
                    }
                });

        //进行握手
        //客户端与服务端连接的通道，final修饰表示只会有一个
        try {
            boot.connect(websocketURI.getHost(), websocketURI.getPort()).sync();
            //阻塞等待是否握手成功
            channel = WebSocketClientHandler.handshakeFuture.sync().channel();
            log.info("WS连接成功:{},id:{}",websocketURI.getHost(),channel.id());
            wsListener.success();
        } catch (Exception e) {
            reConnect();
            log.error("连接异常:{}",e);
        }

    }

    public void send(String msg) {
        channel.writeAndFlush(new TextWebSocketFrame(msg));
    }

    @Override
    public boolean isAlive() {
        return channel == null ? false : channel.isActive();
    }

    public void reConnect(int seconds) {
        group.schedule(()->{
            log.info("准备重新连接:{}",websocketURI.getHost());
            connect();
        },seconds, TimeUnit.SECONDS);
    }

    public void reConnect() {
        reConnect(3);
    }

    @Override
    public void close() {
        if (channel != null) {
            log.info("关闭连接:{}",websocketURI.getHost());
            channel.close();
            channel = null;
            close = true;
        }
    }
}

package com.wsss.market.maker.model.ws.netty;

import io.netty.channel.*;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

@Slf4j
public class WebSocketClientHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    //握手的状态信息
    public static WebSocketClientHandshaker handshaker;
    //netty自带的异步处理
    public static ChannelPromise handshakeFuture;

    public WebSocketClientHandler(URI uri) {
        try {
            this.handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                    uri, WebSocketVersion.V13, null, true, new DefaultHttpHeaders(false),655360);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        Channel ch = ctx.channel();
        //进行握手操作
        if (!this.handshaker.isHandshakeComplete()) {
            try {
                //握手协议返回，设置结束握手
                this.handshaker.finishHandshake(ch, msg);
                //设置成功
                this.handshakeFuture.setSuccess();
            } catch (WebSocketHandshakeException var7) {
                var7.printStackTrace();
            }
        }
    }

    /**
     * Handler活跃状态，表示连接成功
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("与服务端连接成功:{}",ctx.channel().id());
        this.handshaker.handshake(ctx.channel());
    }


    public void handlerAdded(ChannelHandlerContext ctx) {
        this.handshakeFuture = ctx.newPromise();
    }

    public WebSocketClientHandshaker getHandshaker() {
        return handshaker;
    }

    public void setHandshaker(WebSocketClientHandshaker handshaker) {
        this.handshaker = handshaker;
    }

    public ChannelPromise getHandshakeFuture() {
        return handshakeFuture;
    }

    public void setHandshakeFuture(ChannelPromise handshakeFuture) {
        this.handshakeFuture = handshakeFuture;
    }

    public ChannelFuture handshakeFuture() {
        return this.handshakeFuture;
    }
}
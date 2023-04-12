package com.wsss.market.maker.ws.netty;

import io.netty.channel.*;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.util.CharsetUtil;

import java.net.URI;

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
        System.out.println("当前握手的状态" + this.handshaker.isHandshakeComplete());
        Channel ch = ctx.channel();
        System.out.println("服务端的消息" + msg.content().toString(CharsetUtil.UTF_8));
        //进行握手操作
        if (!this.handshaker.isHandshakeComplete()) {
            try {
                //握手协议返回，设置结束握手
                this.handshaker.finishHandshake(ch, msg);
                //设置成功
                this.handshakeFuture.setSuccess();
                System.out.println("服务端的消息" + msg.headers());
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
        System.out.println("与服务端连接成功");
        this.handshaker.handshake(ctx.channel());
    }

    /**
     * 非活跃状态，没有连接远程主机的时候。
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("主机关闭");
    }

    /**
     * 异常处理
     *
     * @param ctx
     * @param cause
     * @throws Exception
     */
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("连接异常：" + cause.getMessage());
        cause.printStackTrace();
        ctx.close();
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
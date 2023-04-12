package com.wsss.market.maker.ws.netty;

import com.wsss.market.maker.ws.WSListener;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.*;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
@ChannelHandler.Sharable
public class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    private WSListener wsListener;
    private NettyWSClient nettyWSClient;

    public WebSocketFrameHandler(WSListener wsListener, NettyWSClient nettyWSClient) {
        this.wsListener = wsListener;
        this.nettyWSClient = nettyWSClient;
    }

    /**
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame msg) throws Exception {
        if (msg instanceof TextWebSocketFrame) {
            String message = ((TextWebSocketFrame) msg).text();
            wsListener.receive(message);
            return;
        }

        if (msg instanceof BinaryWebSocketFrame) {
            byte[] message = ByteBufUtil.getBytes(((BinaryWebSocketFrame) msg).content());
            wsListener.receive(message);
            return;
        }

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if(!nettyWSClient.isClose()) {
            nettyWSClient.reConnect();
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("异常:{}", cause);
        super.exceptionCaught(ctx,cause);
    }
}

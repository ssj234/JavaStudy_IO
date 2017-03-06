package com.ssj.netty;

import java.net.SocketAddress;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPromise;

public class TimeClientHandler extends ChannelInboundHandlerAdapter {

	private final ByteBuf firstMessage;
	
	public TimeClientHandler(){
		byte[] req="QUERY TIME ORDER".getBytes();
		firstMessage=Unpooled.buffer(req.length);
		firstMessage.writeBytes(req);
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		ctx.writeAndFlush(firstMessage);
		
		/** 用来测试粘包和拆包，连续发送100次，有问题
		for(int i=0;i<100;i++){
			ByteBuf firstMessage=Unpooled.buffer(req.length);
			firstMessage.writeBytes(req);
			ctx.writeAndFlush(firstMessage);
		}
		*/
	}
	
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		
		ByteBuf buf=(ByteBuf)msg;
		
		byte[] req=new byte[buf.readableBytes()];
		buf.readBytes(req);
		
		String body=new String(req,"UTF-8");
		System.out.println("Now is : "+body);
		
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		ctx.close();
	}
}

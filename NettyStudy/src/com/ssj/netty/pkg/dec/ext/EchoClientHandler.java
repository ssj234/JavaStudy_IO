package com.ssj.netty.pkg.dec.ext;

import java.net.SocketAddress;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPromise;


public class EchoClientHandler extends ChannelInboundHandlerAdapter {

	private int counter;
	private ByteBuf firstMessage=null;
	static final String ECHO_REQ="Hi,Welcome to Netty.$_";
	public EchoClientHandler(){
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
//		ctx.writeAndFlush(firstMessage);
		//修改点1，连续发送10次数据
		for(int i=0;i<10;i++){
			ByteBuf firstMessage=Unpooled.copiedBuffer(ECHO_REQ.getBytes());
			ctx.writeAndFlush(firstMessage);
		}
	}
	
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		
//		ByteBuf buf=(ByteBuf)msg;
//		byte[] req=new byte[buf.readableBytes()];
//		buf.readBytes(req);
//		String body=new String(req,"UTF-8");
		String body=(String)msg;
		System.out.println("Now is : "+body+"; the counter is "+ ++counter);
		
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		ctx.close();
	}
}

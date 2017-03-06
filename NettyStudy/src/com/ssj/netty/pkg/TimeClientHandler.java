package com.ssj.netty.pkg;

import java.net.SocketAddress;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPromise;

/**
 * 输出结果
 * Now is : BAD ORDER
 * BAD ORDER
 * ; the counter is 1
 * 客户端只收到一条消息，两个BAD ORDER
 * @author shisj
 *
 */
public class TimeClientHandler extends ChannelInboundHandlerAdapter {

	private int counter;
	private ByteBuf firstMessage=null;
	byte[] req;
	public TimeClientHandler(){
		req=("QUERY TIME ORDER"+System.getProperty("line.separator")).getBytes();
//		firstMessage=Unpooled.buffer(req.length);
//		firstMessage.writeBytes(req);
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
//		ctx.writeAndFlush(firstMessage);
		//修改点1，连续发送100次数据
		for(int i=0;i<100;i++){
			firstMessage=Unpooled.buffer(req.length);
			firstMessage.writeBytes(req);
			ctx.writeAndFlush(firstMessage);
		}
	}
	
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		
		ByteBuf buf=(ByteBuf)msg;
		
		byte[] req=new byte[buf.readableBytes()];
		buf.readBytes(req);
		
		String body=new String(req,"UTF-8");
		System.out.println("Now is : "+body+"; the counter is "+ ++counter);
		
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		ctx.close();
	}
}

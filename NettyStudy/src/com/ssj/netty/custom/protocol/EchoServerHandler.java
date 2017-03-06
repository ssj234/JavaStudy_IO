package com.ssj.netty.custom.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.Date;

/**
 * 输出结果
 * Now is : BAD ORDERBAD ORDER; the counter is 1
 * 客户端只收到一条消息，两个BAD ORDER
 * @author shisj
 *
 */
public class EchoServerHandler extends ChannelInboundHandlerAdapter  {
	
	int counter=0;
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		CMessage cMsg=(CMessage)msg;
		System.out.println("The time server receive order : "+cMsg.getData()+"  ; the counter is :"+ ++counter);
//		body=body+"$_";
//		ByteBuf resp=Unpooled.copiedBuffer(body.getBytes());
//		ctx.write(resp);//只是写入缓冲区
			
	}
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();//通过网络发送
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		cause.printStackTrace();
        ctx.close();
	}
}

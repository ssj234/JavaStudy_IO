package com.ssj.netty.pkg.dec;

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
public class TimeServerHandler extends ChannelInboundHandlerAdapter  {
	
	int counter=0;
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
//		ByteBuf buf=(ByteBuf) msg;//将msg转换成Netty的ByteBuf对象
//		byte[] req=new byte[buf.readableBytes()];
//		buf.readBytes(req);
		String body=(String)msg;
//		String body=new String(req,"GBK").substring(0,req.length-System.getProperty("line.separator").length());
		System.out.println("The time server receive order : "+body+"; the counter is :"+ ++counter);
		String currentTime="QUERY TIME ORDER".equalsIgnoreCase(body)?new Date(System.currentTimeMillis()).toString():"BAD ORDER";
		currentTime=currentTime+System.getProperty("line.separator");
		ByteBuf resp=Unpooled.copiedBuffer(currentTime.getBytes());
		ctx.write(resp);//只是写入缓冲区
			
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

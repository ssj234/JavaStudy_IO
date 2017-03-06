package com.ssj.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;

import java.util.Date;

/**
 * 
 * @author shisj
 *
 */
public class TimeServerHandler extends ChannelInboundHandlerAdapter  {
	
//	public void channelActive(final ChannelHandlerContext ctx) throws Exception {
//		final ByteBuf time=ctx.alloc().buffer(41);
//		time.writeBytes("是飒飒".getBytes("GBK"));
//		
//		final ChannelFuture f=ctx.writeAndFlush(time);
//		f.addListener(new ChannelFutureListener() {
//			
//			@Override
//			public void operationComplete(ChannelFuture future) throws Exception {
//				assert f==future;
//				ctx.close();
//			}
//		});
//	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		ByteBuf buf=(ByteBuf) msg;//将msg转换成Netty的ByteBuf对象
		byte[] req=new byte[buf.readableBytes()];
		
		buf.readBytes(req);
		String body=new String(req,"GBK");
		System.out.println("The time server receive order : "+body);
		String currentTime="QUERY TIME ORDER".equalsIgnoreCase(body)?new Date(System.currentTimeMillis()).toString():"BAD ORDER";
		ByteBuf resp=Unpooled.copiedBuffer(currentTime.getBytes());
		ctx.write(resp);//只是写入缓冲区
			
//		ReferenceCountUtil.release(msg);
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

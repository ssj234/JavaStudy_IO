package com.ssj.netty.custom.protocol;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

public class EchoClient {

	
	public void connect(int port,String host) throws Exception{
		EventLoopGroup group=new NioEventLoopGroup();
		try{
			Bootstrap b=new Bootstrap();
			b.group(group)
				.channel(NioSocketChannel.class)
				.option(ChannelOption.TCP_NODELAY, true)
				.handler(new ChannelInitializer<SocketChannel>(){
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new HeaderDecoder());
						ch.pipeline().addLast(new HeaderEncoder());
						ch.pipeline().addLast(new EchoClientHandler());
					}
					
				});
			
			ChannelFuture f=b.connect(host,port).sync();
			f.channel().closeFuture().sync();
		}finally{
			group.shutdownGracefully();
		}
		
	}
	
	
	public static void main(String[] args) {
		try {
			new EchoClient().connect(9898, "127.0.0.1");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}

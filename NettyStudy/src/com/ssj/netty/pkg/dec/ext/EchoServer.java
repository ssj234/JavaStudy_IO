package com.ssj.netty.pkg.dec.ext;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;


/**
 * 新增了两个解码器
 * ch.pipeline().addLast(new LineBasedFrameDecoder(1024));//增加了解码器
 * ch.pipeline().addLast(new StringDecoder());//增加了解码器
 * @author shisj
 *
 */
public class EchoServer {

	
	public void bind(int port) throws Exception{
		//创建两个线程组,专门用于网络事件的处理，Reactor线程组
		//一个用来接收客户端的连接，
		//一个用来进行SocketChannel的网络读写
		EventLoopGroup bossGroup=new NioEventLoopGroup();
		EventLoopGroup workGroup=new NioEventLoopGroup();
		
		try{
			//辅助启动类
			ServerBootstrap b=new ServerBootstrap();
			b.group(bossGroup,workGroup)
				.channel(NioServerSocketChannel.class)//创建的channel为NioServerSocketChannel【nio-ServerSocketChannel】
				.option(ChannelOption.SO_BACKLOG, 1024)
				.childOption(ChannelOption.SO_KEEPALIVE, true) //配置accepted的channel
				.childHandler(new ChannelInitializer<Channel>() {
					@Override
					protected void initChannel(Channel ch) throws Exception {
						ByteBuf delimiter=Unpooled.copiedBuffer("$_".getBytes());//分隔符
						//1024,单条消息最大长度，超过仍未发现分隔符，抛出异常TooLongFrameException
//						ch.pipeline().addLast(new DelimiterBasedFrameDecoder(1024, delimiter));
						ch.pipeline().addLast(new FixedLengthFrameDecoder(20));
						ch.pipeline().addLast(new StringDecoder());
						ch.pipeline().addLast(new EchoServerHandler());
					}
					
				});//处理IO事件的处理类，处理网络事件
			ChannelFuture f=b.bind(port).sync();//绑定端口后同步等待
			f.channel().closeFuture().sync();//阻塞
		}catch(Exception e){
			bossGroup.shutdownGracefully();
			workGroup.shutdownGracefully();
		}
	}
	
	
	public static void main(String[] args) {
		try {
			new EchoServer().bind(9898);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}

 class ChildChannelHandler extends ChannelInitializer<SocketChannel> {

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ch.pipeline().addLast(new LineBasedFrameDecoder(1024));//增加了解码器
		ch.pipeline().addLast(new StringDecoder());//增加了解码器
		ch.pipeline().addLast(new EchoServerHandler());
	}
	
	
}
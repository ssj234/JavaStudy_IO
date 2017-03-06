package net.ssj.aio;

import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.CountDownLatch;

/**
 * 服务器程序
 * @author shishengjie
 *
 */
public class AsyncTimeServerHandler implements Runnable {

	private int port;
	CountDownLatch latch;
	
	//AIO的服务器channel
	AsynchronousServerSocketChannel asynchronousServerSocketChannel;
	
	public AsyncTimeServerHandler(int port){
		this.port=port;
		try{
			//创建一个异步的服务端通道
			asynchronousServerSocketChannel=AsynchronousServerSocketChannel.open();
			//监听端口
			asynchronousServerSocketChannel.bind(new InetSocketAddress(port));
			System.out.println("The time server is start in port : "+port);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		latch=new CountDownLatch(1);
		doAccept();//接受请求
		System.out.println("doAccept() registe the AcceptCompletionHandler!");
		try{
			latch.await();
		}catch(InterruptedException e){
			e.printStackTrace();
		}
	}

	private void doAccept() {
		//接受请求时，只需要注册一个接受到请求后的处理器即可【CompletionHandler】
		asynchronousServerSocketChannel.accept(this, new AcceptCompletionHandler());
	}

}

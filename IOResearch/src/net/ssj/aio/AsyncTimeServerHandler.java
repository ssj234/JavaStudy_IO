package net.ssj.aio;

import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.CountDownLatch;

/**
 * ����������
 * @author shishengjie
 *
 */
public class AsyncTimeServerHandler implements Runnable {

	private int port;
	CountDownLatch latch;
	
	//AIO�ķ�����channel
	AsynchronousServerSocketChannel asynchronousServerSocketChannel;
	
	public AsyncTimeServerHandler(int port){
		this.port=port;
		try{
			//����һ���첽�ķ����ͨ��
			asynchronousServerSocketChannel=AsynchronousServerSocketChannel.open();
			//�����˿�
			asynchronousServerSocketChannel.bind(new InetSocketAddress(port));
			System.out.println("The time server is start in port : "+port);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		latch=new CountDownLatch(1);
		doAccept();//��������
		System.out.println("doAccept() registe the AcceptCompletionHandler!");
		try{
			latch.await();
		}catch(InterruptedException e){
			e.printStackTrace();
		}
	}

	private void doAccept() {
		//��������ʱ��ֻ��Ҫע��һ�����ܵ������Ĵ��������ɡ�CompletionHandler��
		asynchronousServerSocketChannel.accept(this, new AcceptCompletionHandler());
	}

}

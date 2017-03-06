package net.ssj.aio.client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;

/**
 * 客户端
 * @author shishengjie
 *
 */
public class AsyncTimeClientHandler implements Runnable,CompletionHandler<Void, AsyncTimeClientHandler> {

	private String host;
	private int port;
	private CountDownLatch latch;
	private AsynchronousSocketChannel client;//客户端socket
	
	public AsyncTimeClientHandler(String host, int port) {
		this.host=host;
		this.port=port;
		
		try {
			this.client=AsynchronousSocketChannel.open();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		
		latch=new CountDownLatch(1);
		//客户端连接地址，交给本CompletionHandler处理
		client.connect(new InetSocketAddress(host,port),this,this);
		
		try {
			latch.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			client.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public void completed(Void result, AsyncTimeClientHandler attachment) {
		byte[] req="QUERY TIME ORDER".getBytes();//连接上，开始写入
		ByteBuffer writeBuffer=ByteBuffer.allocate(req.length);
		writeBuffer.put(req);
		writeBuffer.flip();
		
		client.write(writeBuffer,writeBuffer,
				new CompletionHandler<Integer, ByteBuffer>() {

					@Override
					public void completed(Integer result, ByteBuffer buffer) {
						//写入完成，
						if(buffer.hasRemaining()){
							client.write(buffer,buffer,this);
						}else{
							ByteBuffer readBuffer=ByteBuffer.allocate(1024);
							//读取
							client.read(readBuffer,readBuffer,
									new CompletionHandler<Integer, ByteBuffer>() {

										@Override
										public void completed(Integer result,
												ByteBuffer buffer) {
											buffer.flip();
											byte [] bytes=new byte[buffer.remaining()];
											buffer.get(bytes);
											
											String body = null;
										
											try {
												body=new String(bytes,"UTF-8");
											} catch (UnsupportedEncodingException e) {
												// TODO Auto-generated catch block
												e.printStackTrace();
											}
											System.out.println("Now is : "+body);
											latch.countDown();
										}

										@Override
										public void failed(Throwable exc,
												ByteBuffer buffer) {
											try {
												client.close();
												latch.countDown();
											} catch (IOException e) {
												// TODO Auto-generated catch block
												e.printStackTrace();
											}
											
										}
									});
							
						}
						
					}

					@Override
					public void failed(Throwable exc, ByteBuffer attachment) {
						try {
							client.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						latch.countDown();
					}
				});
		
		
		
		
		
	}

	@Override
	public void failed(Throwable exc, AsyncTimeClientHandler attachment) {
		exc.printStackTrace();
		try {
			client.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		latch.countDown();
	}

}

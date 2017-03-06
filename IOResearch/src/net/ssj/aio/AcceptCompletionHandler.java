package net.ssj.aio;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;


/**
 * 用于处理接受请求
 * 主要有两个方法：
 * completed
 * failed
 *
 */
@SuppressWarnings("hiding")
public class AcceptCompletionHandler  
			implements CompletionHandler<AsynchronousSocketChannel,AsyncTimeServerHandler>{

	@Override
	public void completed(AsynchronousSocketChannel result,
			AsyncTimeServerHandler attachment) {
		//若接受完成，调用服务器channel的accept
		attachment.asynchronousServerSocketChannel.accept(attachment,this);
		//分配一个缓冲区，读取输入，交给ReadCompletionHandler处理【CompletionHandler】
		ByteBuffer buffer=ByteBuffer.allocate(1024);
		result.read(buffer,buffer,new ReadCompletionHandler(result));
		
	}

	@Override
	public void failed(Throwable exc, AsyncTimeServerHandler attachment) {
		exc.printStackTrace();
		attachment.latch.countDown();
	}


	

}

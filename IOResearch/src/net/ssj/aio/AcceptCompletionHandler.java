package net.ssj.aio;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;


/**
 * ���ڴ����������
 * ��Ҫ������������
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
		//��������ɣ����÷�����channel��accept
		attachment.asynchronousServerSocketChannel.accept(attachment,this);
		//����һ������������ȡ���룬����ReadCompletionHandler����CompletionHandler��
		ByteBuffer buffer=ByteBuffer.allocate(1024);
		result.read(buffer,buffer,new ReadCompletionHandler(result));
		
	}

	@Override
	public void failed(Throwable exc, AsyncTimeServerHandler attachment) {
		exc.printStackTrace();
		attachment.latch.countDown();
	}


	

}

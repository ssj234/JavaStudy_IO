package net.ssj.nio.nt;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

/**
 * 时间服务器
 * @author shishengjie
 *
 */
public class MultiplexerTimeServer implements Runnable {
	private Selector selector;
	private ServerSocketChannel servChannel;
	
	private volatile boolean stop;
	
	public MultiplexerTimeServer(){
		try{
			selector=Selector.open();
			servChannel=ServerSocketChannel.open();
			servChannel.configureBlocking(false);//设置为非阻塞模式
			servChannel.socket().bind(new InetSocketAddress(9898),1024);
			servChannel.register(selector, SelectionKey.OP_ACCEPT);//注册到selector，监听accept事件
			System.out.println("The time server is start at port 9898!");
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	public void stop(){
		this.stop=true;
	}
	
	@Override
	public void run() {
		while(!stop){
			try{
				int size=selector.select();//休眠事件为1s，每1s唤醒一次
				System.out.println("size is"+size);
				Set<SelectionKey> selectedKeys=selector.selectedKeys();
				Iterator<SelectionKey> it=selectedKeys.iterator();
				SelectionKey key=null;
				while(it.hasNext()){
					key=it.next();
					it.remove();
					
					try{
						handleInput(key);
					}catch(Exception e){
						e.printStackTrace();
						if(key!=null){
							key.cancel();
							if(key.channel()!=null){
								key.channel().close();
							}
						}
						
					}
				}
				
				
				
			}catch(Exception e){
				e.printStackTrace();
			}
			
		}
		
		if(selector!=null){
			try{
				selector.close();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}

	private void handleInput(SelectionKey key) throws IOException {
		if(key.isValid()){
			if(key.isAcceptable()){
				ServerSocketChannel ssc=(ServerSocketChannel) key.channel();
				SocketChannel sc=ssc.accept();
				sc.configureBlocking(false);
				sc.register(selector, SelectionKey.OP_READ);
			}
			
			if(key.isReadable()){
				SocketChannel sc=(SocketChannel)key.channel();
				ByteBuffer readBuffer=ByteBuffer.allocate(1024);
				int readBytes=sc.read(readBuffer);
				if(readBytes>0){
					readBuffer.flip();
					byte[] bytes=new byte[readBuffer.remaining()];
					readBuffer.get(bytes);
					
					String body=new String(bytes,"UTF-8");
					System.out.println("The time server receive order:"+body);
					
					String currentTime="QUERY TIME ORDER".equalsIgnoreCase(body)?new Date(System.currentTimeMillis()).toString():"BAD ORDER";
					
					doWrite(sc,currentTime);
				}else if(readBytes<0){
					key.cancel();
					sc.close();
				}
				
			}
		}
	}

	private void doWrite(SocketChannel channel, String response) throws IOException {
		if(response!=null&&response.trim().length()>0){
			
			byte[] bytes=response.getBytes();
			ByteBuffer writeBuffer=ByteBuffer.allocate(bytes.length);
			writeBuffer.put(bytes);
			writeBuffer.flip();
			channel.write(writeBuffer);
		}
		
	}
	
}

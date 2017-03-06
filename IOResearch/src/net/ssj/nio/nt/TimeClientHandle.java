package net.ssj.nio.nt;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * netty书中的例子
 * @author shishengjie
 *
 */
public class TimeClientHandle implements Runnable {

	private String host;
	private Selector selector;
	private SocketChannel socketChannel;
	private int port;
	private volatile boolean stop;
	
	public TimeClientHandle(int port){
		this.port=port;
		try{
			selector=Selector.open();
			socketChannel=SocketChannel.open();
			socketChannel.configureBlocking(false);
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	@Override
	public void run() {
		try{
			doConnect();//连接
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
		
		while(!stop){//若线程不结束，一直继续
			try{
				selector.select(1000);//选择
				Set<SelectionKey> selectedKeys=selector.selectedKeys();
				Iterator<SelectionKey> it=selectedKeys.iterator();
				SelectionKey key=null;
				while(it.hasNext()){
					key=it.next();
					it.remove();//注意，需要移除
					try{
						handleInput(key);//处理选择出的连接
					}catch(Exception e){
						if(key!=null){
							key.cancel();
							if(key.channel()!=null){
								key.channel().close();
							}
						}
						
					}
				}
			}catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
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
	
	/**
	 * 连接服务器，若连接成功，写入数据；若连接失败，使用OP_CONNECT注册到选择器上；
	 * @throws IOException
	 */
	private void doConnect() throws IOException {
		if(socketChannel.connect(new InetSocketAddress(port))){
			socketChannel.register(selector, SelectionKey.OP_READ);
			doWrite(socketChannel);
		}else{
			socketChannel.register(selector,  SelectionKey.OP_CONNECT);
		}
		
	}
	private void doWrite(SocketChannel sc) throws IOException {
		byte[] req="QUERY TIME ORDER".getBytes();
		ByteBuffer writeBuffer=ByteBuffer.allocate(req.length);
		writeBuffer.put(req);
		writeBuffer.flip();
		sc.write(writeBuffer);
		if(!writeBuffer.hasRemaining()){
			System.out.println("Send order 2 server succeed.");
		}
		
		
	}
	private void handleInput(SelectionKey key) throws IOException {
		if(key.isValid()){//若连接可用
			SocketChannel sc=(SocketChannel)key.channel();//获取连接的channel
			
			if(key.isConnectable()){//若为连接事件，补doWrite，与doConnection()一致
				if(sc.finishConnect()){
					sc.register(selector, SelectionKey.OP_READ);
					doWrite(sc);
				}else{
					System.exit(1);//连接失败，退出
				}
			}
			
			if(key.isReadable()){//若是读事件
				ByteBuffer readBuffer=ByteBuffer.allocate(1024);
				int readBytes=sc.read(readBuffer);//读取到缓存
				if(readBytes>0){
					
					readBuffer.flip();//翻转
					byte[] bytes=new byte[readBuffer.remaining()];
					readBuffer.get(bytes);//获取数据
					String body=new String(bytes,"UTF-8");
					System.out.println("Now is: "+ body);
					this.stop=true;
					key.cancel();//要结束了，
					sc.close();
				}else if(readBytes<0){
					key.cancel();
					sc.close();
				}
				
			}
			
		}
		
	}


}

package net.ssj.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class NIOServer {

	
	public void start() throws Exception{
		
		ServerSocketChannel serverChan=ServerSocketChannel.open();
		serverChan.configureBlocking(false);
		ServerSocket serverSock=serverChan.socket();
		serverSock.bind(new InetSocketAddress(9898));
		
		Selector selector=Selector.open();
		
		
		serverChan.register(selector, SelectionKey.OP_ACCEPT);
		
		ByteBuffer buf=ByteBuffer.allocate(48);
		buf.put("Hello World!".getBytes());
		
		while(true){
			int size=selector.select();
			System.out.println("size="+size);
			if(size==0){
				continue;
			}
			Iterator it = selector.selectedKeys().iterator();
			while(it.hasNext()){
				SelectionKey key=(SelectionKey) it.next();
				System.out.println("accetp:"+key.isAcceptable()+" "+key.isWritable());
				if(key.isAcceptable()){
					ServerSocketChannel chann= (ServerSocketChannel) key.channel();
					SocketChannel client=chann.accept();
					client.configureBlocking(false);
					client.register(selector, SelectionKey.OP_WRITE);
					buf.flip();
					client.write(buf);
				}
				
				it.remove();//
			}
			
		}
		
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			new NIOServer().start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

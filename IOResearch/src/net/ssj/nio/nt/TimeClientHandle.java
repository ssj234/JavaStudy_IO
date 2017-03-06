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
 * netty���е�����
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
			doConnect();//����
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
		
		while(!stop){//���̲߳�������һֱ����
			try{
				selector.select(1000);//ѡ��
				Set<SelectionKey> selectedKeys=selector.selectedKeys();
				Iterator<SelectionKey> it=selectedKeys.iterator();
				SelectionKey key=null;
				while(it.hasNext()){
					key=it.next();
					it.remove();//ע�⣬��Ҫ�Ƴ�
					try{
						handleInput(key);//����ѡ���������
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
	 * ���ӷ������������ӳɹ���д�����ݣ�������ʧ�ܣ�ʹ��OP_CONNECTע�ᵽѡ�����ϣ�
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
		if(key.isValid()){//�����ӿ���
			SocketChannel sc=(SocketChannel)key.channel();//��ȡ���ӵ�channel
			
			if(key.isConnectable()){//��Ϊ�����¼�����doWrite����doConnection()һ��
				if(sc.finishConnect()){
					sc.register(selector, SelectionKey.OP_READ);
					doWrite(sc);
				}else{
					System.exit(1);//����ʧ�ܣ��˳�
				}
			}
			
			if(key.isReadable()){//���Ƕ��¼�
				ByteBuffer readBuffer=ByteBuffer.allocate(1024);
				int readBytes=sc.read(readBuffer);//��ȡ������
				if(readBytes>0){
					
					readBuffer.flip();//��ת
					byte[] bytes=new byte[readBuffer.remaining()];
					readBuffer.get(bytes);//��ȡ����
					String body=new String(bytes,"UTF-8");
					System.out.println("Now is: "+ body);
					this.stop=true;
					key.cancel();//Ҫ�����ˣ�
					sc.close();
				}else if(readBytes<0){
					key.cancel();
					sc.close();
				}
				
			}
			
		}
		
	}


}

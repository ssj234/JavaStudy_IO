package net.ssj.executor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import net.ssj.pool.CommonThreadPool;

/**
 * �����Գ�������ʹ��BIO + �̳߳صķ�ʽ�������� �̳߳ش�СΪ��5
 * ab -n 10 -c 10  http://127.0.0.1:9898/
 * n����10������-cģ��10�������൱10��ͬʱ����	��ʱ��100% 10
 *  ���ӵ�50��ͬʱ���ʣ���ʱ��100% 42
 *  ���ӵ�100��ͬʱ���ʣ���ʱ��100% 72
 *  ���ӵ�200��ͬʱ���ʣ���ʱ��100% 190
 * @author shishengjie
 *
 */
public class BioServer {

	ServerSocket serverSock=null;
	CommonThreadPool<BioHandler> pool=null;
	
	public BioServer(){
		try {
			serverSock=new ServerSocket();
			pool=new CommonThreadPool<>();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void start() throws Exception{
		serverSock.bind(new InetSocketAddress(9898));//�󶨶Ͽ�9898
		System.out.println("Listening on port 9898!");
		while(true){
			Socket socket= serverSock.accept();
//			System.out.println("Accept from a client!");
			pool.execute(new BioHandler(socket));
		}
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		BioServer bio=new  BioServer();
		bio.start();
	}

}

package net.ssj.bio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * �����Գ�������ʹ��BIO + �߳�Hanlder��ÿ�δ��������߳�
 * ab -n 10 -c 10  http://127.0.0.1:9898/
 * 	n����10������-cģ��10�������൱10��ͬʱ����	��ʱ��100% 13
 *  ���ӵ�50��ͬʱ���ʣ���ʱ��100% 48
 *  ���ӵ�100��ͬʱ���ʣ���ʱ��100% 87
 *  ���ӵ�200��ͬʱ���ʣ���ʱ��100% 181
 *  
 *  ���ۣ��������������ӣ���Ϊ��ʱ�������̡߳��ٸߵĻ��ͱ���apr_socket_recv: ���������е������ֹ��һ���ѽ��������ӡ� 
 * @author shishengjie
 *
 */
public class BioServer {

	ServerSocket serverSock=null;
	
	public BioServer(){
		try {
			serverSock=new ServerSocket();
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
			System.out.println("Accept from a client!");
			BioHandler handler=new BioHandler(socket);
			handler.start();
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

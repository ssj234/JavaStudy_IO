package net.ssj.bio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 【测试场景】：使用BIO + 线程Hanlder，每次创建处理线程
 * ab -n 10 -c 10  http://127.0.0.1:9898/
 * 	n发出10个请求，-c模拟10并发，相当10人同时访问	耗时：100% 13
 *  增加到50人同时访问，耗时：100% 48
 *  增加到100人同时访问，耗时：100% 87
 *  增加到200人同时访问，耗时：100% 181
 *  
 *  结论：基本上线性增加，因为随时创建的线程。再高的话就报错：apr_socket_recv: 您的主机中的软件中止了一个已建立的连接。 
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
		serverSock.bind(new InetSocketAddress(9898));//绑定断开9898
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

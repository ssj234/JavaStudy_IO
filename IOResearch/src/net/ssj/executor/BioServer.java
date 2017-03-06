package net.ssj.executor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import net.ssj.pool.CommonThreadPool;

/**
 * 【测试场景】：使用BIO + 线程池的方式处理请求， 线程池大小为：5
 * ab -n 10 -c 10  http://127.0.0.1:9898/
 * n发出10个请求，-c模拟10并发，相当10人同时访问	耗时：100% 10
 *  增加到50人同时访问，耗时：100% 42
 *  增加到100人同时访问，耗时：100% 72
 *  增加到200人同时访问，耗时：100% 190
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
		serverSock.bind(new InetSocketAddress(9898));//绑定断开9898
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

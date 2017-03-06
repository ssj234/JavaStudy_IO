package net.ssj.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ClientThread thread=new ClientThread();
		List<Thread> rs=new ArrayList<Thread>(); 
		int count=1;//000;
		for(int i=0;i<count;i++){
			Thread t1=new Thread(thread,"Thread-"+i);
			rs.add(t1);
		}
		for(Thread t1:rs){
			t1.start();
			try {
				t1.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println("time per request "+(ClientThread.sum/ClientThread.count));
	}

}
class ClientThread implements Runnable{
	public static volatile float sum=0;
	public static volatile float count=0;
	@Override
	public void run() {
		long begin=System.currentTimeMillis();
		
		Socket socket=new Socket();
		try {
			char chs[]=new char[128];
			socket.connect(new InetSocketAddress(9898));
			InputStream in=socket.getInputStream();
			InputStreamReader reader=new InputStreamReader(in);
			int i=reader.read(chs);
			System.out.println(" str="+new String(chs,0,i));
			
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			long end=System.currentTimeMillis();
			System.out.println(Thread.currentThread().getName()+":"+(end)+" "+begin);
			sum=sum+(end-begin);
			count++;
			try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
}

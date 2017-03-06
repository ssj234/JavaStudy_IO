package net.ssj.bio.uspool;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class BioHandler extends Thread {
	Socket socket;
	public BioHandler(Socket socket){
		this.socket=socket;
	}

	@Override
	public void run() {
		try {
			OutputStream out=this.socket.getOutputStream();
			out.write("Hello World!".getBytes());
			out.close();
			this.socket.close();
			System.out.println(Thread.currentThread().getName()+" handle request completed!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

package net.ssj.bio;

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
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

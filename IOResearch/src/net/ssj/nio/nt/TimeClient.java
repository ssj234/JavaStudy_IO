package net.ssj.nio.nt;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TimeClient {

	public static void main(String[] args) throws InterruptedException {
		
		Thread a=new Thread(new TimeClientHandle(9898),"TimeClient-001");
		a.start();
	}

}

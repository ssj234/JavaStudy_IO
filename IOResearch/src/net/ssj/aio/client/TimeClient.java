package net.ssj.aio.client;

public class TimeClient {

	public static void main(String[] args) {
		new Thread(new AsyncTimeClientHandler("127.0.0.1",9898),"AIO-AsyncTimeClientHandler-001").start();
	}
}

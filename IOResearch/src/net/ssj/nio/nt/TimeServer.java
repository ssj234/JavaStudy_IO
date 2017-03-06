package net.ssj.nio.nt;

public class TimeServer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MultiplexerTimeServer server=new MultiplexerTimeServer();
		new Thread(server,"NIO-MultiplexerTimeServer-001").start();
	}

}

package net.ssj.aio;

public class TimeServer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int port=9898;
		//����������
		AsyncTimeServerHandler timeServer=new AsyncTimeServerHandler(port);
		new Thread(timeServer,"AIO-AsyncTimeServerHandler-001").start();
	}

}

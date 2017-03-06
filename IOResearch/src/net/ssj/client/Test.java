package net.ssj.client;

public class Test {

	public static void main(String[] args) {
		for(int i=0;i<10;i++){
			MyThread thread=new MyThread();
			thread.start();
		}
	}

	
}

class MyThread extends Thread{
	
	public static volatile int sum=0;
	
	@Override
	public void run() {
		sum++;
		System.out.println("sum is "+sum);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
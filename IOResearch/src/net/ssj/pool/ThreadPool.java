package net.ssj.pool;

public interface ThreadPool<Job extends Runnable> {
	
	//ִ��һ��Job�����Job��Ҫʵ��Runnable�ӿ�
	void execute(Job job);
	//�ر��̳߳�
	void shutdown();
	//���ӹ������߳�
	void addWorkers(int num);
	//���ٹ������߳�
	void removeWorker(int num);
	//�õ����ڵȴ�ִ�е���������
	int getJobSize();
}

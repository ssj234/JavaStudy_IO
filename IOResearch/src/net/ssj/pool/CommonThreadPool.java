package net.ssj.pool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class CommonThreadPool<Job extends Runnable> implements ThreadPool<Job> {

	// �̳߳����������
	private static final int MAX_WORKER_NUMBERS = 100;
	// �̳߳�Ĭ�ϵ�����
	private static final int DEFAULT_WORKER_NUMBERS = 1;
	// �̳߳���С����
	private static final int MIN_WORKER_NUMBERS = 1;
	// �����б�
	private final LinkedList<Job> jobs = new LinkedList<Job>();
	// �������б�,synchronizedList���̰߳�ȫ���ܽ���ָ�������ֱ��ʹ�����ṩ�ĺ��������磺queue.add(obj); ����
	// queue.poll(obj);�����������Լ�����Ҫ���κ�ͬ����
	private final List<Worker> workers = Collections
			.synchronizedList(new ArrayList<Worker>());
	// �������߳�����
	// private int workerNum=DEFAULT_WORKER_NUMBERS;
	// �̱߳��
	private AtomicLong threadNum = new AtomicLong();

	public CommonThreadPool() {
		initializeWokers(DEFAULT_WORKER_NUMBERS);
	}

	/**
	 * ��ʼ���̳߳�
	 */
	private void initializeWokers(int num) {
		// ��������̣߳�����workers�У�������
		for (int i = 0; i < num; i++) {
			Worker worker = new Worker();
			workers.add(worker);
			Thread thread = new Thread(worker, "ThreadPool-Worker-"
					+ threadNum.getAndIncrement());
			thread.start();
		}
	}

	@Override
	public void execute(Job job) {
		if (job == null)
			return;
		synchronized (jobs) {
			jobs.addLast(job);
			jobs.notify();
		}
	}

	@Override
	public void shutdown() {
		for (Worker worker : workers) {
			worker.shutdown();
		}
	}

	@Override
	public void addWorkers(int num) {
		synchronized (jobs) {
			int size = workers.size();
			if (num + size > MAX_WORKER_NUMBERS) {// ��Ӻ�Ĵ�С�������ֵ
				num = MAX_WORKER_NUMBERS - size;// ����Ҫ������worker����
			}
			initializeWokers(num);// ��ʼ����num��worker
		}
	}

	@Override
	public void removeWorker(int num) {
		synchronized (jobs) {
			if (num >= this.workers.size()) {
				throw new IllegalArgumentException("beyond workNum!");
			}
			int count = 0;
			while (count < num) {
				Worker worker = workers.get(count);
				if (workers.remove(worker)) {
					worker.shutdown();
					count++;
				}
			}

		}
	}

	@Override
	public int getJobSize() {
		// TODO Auto-generated method stub
		return jobs.size();
	}

	class Worker implements Runnable {

		private volatile boolean running = true;

		public void shutdown() {
			running = false;
		}

		@Override
		public void run() {

			while (running) {
				Job job = null;
				synchronized (jobs) {
					while (jobs.isEmpty()) {// ���jobs�ǿյģ���ִ��jobs.wait��ʹ��while������if����Ϊwait������Ѿ�Ϊ���ˣ������ȴ�
						try {
							jobs.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
							Thread.currentThread().interrupt();// �ж�
							return;// ����
						}
					}
					job = jobs.removeFirst();// ��һ��job
					if (job != null) {
						try {
							job.run();//ע�⣬������run������start�������Job
						} catch (Exception e) {
							// ����Jobִ���е�Exception
							e.printStackTrace();
						}
					}
				}
			}

		}

	}
}

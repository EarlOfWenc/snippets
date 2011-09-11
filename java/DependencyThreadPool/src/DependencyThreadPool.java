import java.util.ArrayList;


public class DependencyThreadPool {
	private BlockingDependencyQueue threadQueue;
	private ArrayList<Thread> threads;
	private volatile boolean shutdown = false;

	private final OrderableRunnable queueFlusher = new OrderableRunnable(null) {

		@Override
		protected void implRun() {
		}
	};

	private class WorkerThread extends Thread
	{
		@Override
		public void run()
		{
			while(true)
			{
				try{
					Runnable r = DependencyThreadPool.this.threadQueue.take();
					if(r == DependencyThreadPool.this.queueFlusher)
						return;
					r.run();
				} catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	public DependencyThreadPool(int threadNum)
	{
		this.threadQueue = new BlockingDependencyQueue();
		// we can't use ThreadPoolExecutor or others since they bypass the queue >.<

		this.threads = new ArrayList<Thread>(threadNum);
		for(int i=0; i<threadNum; ++i)
		{
			this.threads.add(new WorkerThread());
		}

		for(int i=0; i<threadNum; ++i)
		{
			this.threads.get(i).start();
		}
	}
	public DependencyThreadPool()
	{
		this(Runtime.getRuntime().availableProcessors());
	}

	public void execute(OrderableRunnable r) {
		if(this.shutdown)
			throw new IllegalStateException("Already shut down");
		r.setQueue(this.threadQueue);
		this.threadQueue.add(r);
	}

	public void shutdown() {
		if(this.shutdown)
			return;
		this.shutdown = true;
		int exitCount = this.threads.size();
		for(int i=0; i<exitCount; ++i)
			this.threadQueue.add(this.queueFlusher);
	}

}

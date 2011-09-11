
public abstract class OrderableRunnable implements Runnable {
	private BlockingDependencyQueue queue;
	private Object syncObj;

	/**
	 * syncObj: The object this task is synchronized with. Two tasks,
	 * sync to the same object, will be executed in strict order.
	 * Supply null if order doesn't matter.
	 */
	protected OrderableRunnable(Object syncObj)
	{
		this.syncObj = syncObj;
	}

	public final void run()
	{
		try
		{
			this.implRun();
		}
		finally
		{
			if(this.getSyncObj() != null)
				this.queue.finishedTask(this);
		}
	}



	public final Object getSyncObj()
	{
		return this.syncObj;
	}

	protected abstract void implRun();

	public void setQueue(BlockingDependencyQueue threadQueue) {
		this.queue = threadQueue;
	}

}

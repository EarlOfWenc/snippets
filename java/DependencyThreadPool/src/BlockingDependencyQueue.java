import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;


public class BlockingDependencyQueue{

	private HashMap<Object, LinkedList<OrderableRunnable>>  orderedTasks = new HashMap<Object, LinkedList<OrderableRunnable> >();
	private LinkedBlockingQueue<OrderableRunnable> realQueue = new LinkedBlockingQueue<OrderableRunnable>();

	public boolean add(OrderableRunnable arg0) {
		if(arg0 == null || arg0.getSyncObj() == null)
			return this.realQueue.add(arg0);
		this.addOrdered(arg0);
		return true;
	}

	private void addOrdered(OrderableRunnable r)
	{
		assert r.getSyncObj() != null;

		synchronized (this.orderedTasks) {
			LinkedList<OrderableRunnable> list = this.orderedTasks.get(r.getSyncObj());
			if(list == null)
			{
				// nothing else in this class is running. Add us anyway to indicate we are
				// running now
				list = new LinkedList<OrderableRunnable>();
				this.orderedTasks.put(r.getSyncObj(), list);
				this.realQueue.add(r);
			}
			list.add(r);
		}
	}

	public void finishedTask(OrderableRunnable cancelableRunnable) {
		assert cancelableRunnable.getSyncObj() != null;
		OrderableRunnable r;
		synchronized (this.orderedTasks) {
			LinkedList<OrderableRunnable> list = this.orderedTasks.get(cancelableRunnable.getSyncObj());
			if(list == null)
				return;

			// take the object itself rom the queue
			r = list.poll();
			assert r == cancelableRunnable;

			r = list.peek();
			if(r==null)
			{
				this.orderedTasks.remove(cancelableRunnable.getSyncObj());
				return;
			}
			this.realQueue.add(r);
		}
	}

	public Runnable take() throws InterruptedException {
		return this.realQueue.take();
	}

}

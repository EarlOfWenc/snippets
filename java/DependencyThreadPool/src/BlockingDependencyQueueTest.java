import java.util.HashSet;
import java.util.Set;

public class BlockingDependencyQueueTest {
	volatile boolean runningTypeOne = false;
	volatile boolean runningTypeThree = false;
	volatile boolean finished = false;
	volatile boolean concRun = false;
	Set<OrderableRunnable> running = new HashSet<OrderableRunnable>();
	private DependencyThreadPool p;

	static int heavyCalc()
	{
		int sum=0;
		for(int i=0; i<Integer.MAX_VALUE; ++i)
		{
			sum += i;
		}
		// return sth. to prevent clever compilers from removing everything
		return sum;
	}

	class TaskTypeThree extends OrderableRunnable
	{
		private int i;

		protected TaskTypeThree(int i) {
			super(TaskTypeThree.class);
			this.i = i;
		}

		public String toString()
		{
			return "TaskTypeThree "+this.i;
		}
		@Override
		protected void implRun() {
			System.out.println("  started "+this);
			if(BlockingDependencyQueueTest.this.runningTypeThree)
			{
				System.err.println("  Concurrent Run!");
				BlockingDependencyQueueTest.this.concRun = true;
			}
			BlockingDependencyQueueTest.this.runningTypeThree = true;
			heavyCalc();
			System.out.println("  ended"+this);
			BlockingDependencyQueueTest.this.runningTypeThree = false;
			BlockingDependencyQueueTest.this.ended(this);
		}

	}

	class TaskTypeOne extends OrderableRunnable
	{
		private int i;

		protected TaskTypeOne(int i) {
			super(TaskTypeOne.class);
			this.i = i;
		}

		public String toString()
		{
			return "TypeOne "+this.i;
		}
		@Override
		protected void implRun() {
			System.out.println("  started "+this);
			if(BlockingDependencyQueueTest.this.runningTypeOne)
			{
				System.err.println("  Concurrent Run!");
				BlockingDependencyQueueTest.this.concRun = true;
			}
			BlockingDependencyQueueTest.this.runningTypeOne = true;
			heavyCalc();
			System.out.println("  ended"+this);
			BlockingDependencyQueueTest.this.runningTypeOne = false;
			BlockingDependencyQueueTest.this.ended(this);
		}

	}

	class TaskTypeTwo extends OrderableRunnable
	{

		private int i;

		protected TaskTypeTwo(int i) {
			super(null);
			this. i = i;
		}

		public String toString()
		{
			return "TypeTwo "+this.i;
		}

		@Override
		protected void implRun() {
			System.out.println("  started "+this);
			heavyCalc();
			System.out.println("  ended "+this);
			BlockingDependencyQueueTest.this.ended(this);
		}

	}

	public void ended(OrderableRunnable t)
	{
		synchronized (this.running) {
			this.running.remove(t);
			if(this.running.isEmpty())
			{
				this.finished = true;
				this.p.shutdown();
			}
			else
			{
				System.out.println("  still waiting for "+this.running.size()+" tasks");
			}
		}
	}

	public static void main(String[] args) throws Exception {
		BlockingDependencyQueueTest t = new BlockingDependencyQueueTest();
		t.run();
		while(!t.finished)
		{
			Thread.sleep(100);
		}
		if(t.concRun)
			throw new Exception("there was a concurrent run!");
		System.out.println("  Finished BlockingDependencyTest succcessfully");

	}

	private void run() {
		this.p = new DependencyThreadPool(3);
		for(int i=0; i<5; ++i)
		{
			TaskTypeOne one = new TaskTypeOne(i);
			TaskTypeTwo two = new TaskTypeTwo(i);
			TaskTypeThree three = new TaskTypeThree(i);
			this.running.add(two);
			this.running.add(one);
			this.running.add(three);
		}
		synchronized (this.running) {
			for(OrderableRunnable r: this.running)
				this.p.execute(r);
		}
	}
}


package org.softwareartisans.util.workgroup;

/*
 Copyright (c) 2013 Software Artisans, LLC
 Author: Kevin Limperos, klimperos@softwareartisans.org

 Permission is hereby granted, free of charge, to any person obtaining
 a copy of this software and associated documentation files (the
 "Software"), to deal in the Software without restriction, including
 without limitation the rights to use, copy, modify, merge, publish,
 distribute, sublicense, and/or sell copies of the Software, and to
 permit persons to whom the Software is furnished to do so, subject to
 the following conditions:

 The above copyright notice and this permission notice shall be
 included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.softwareartisans.util.workgroup.retry.ImmediateCounting;
import org.softwareartisans.util.workgroup.retry.Retrier;
import org.softwareartisans.util.workgroup.retry.TimedRetrierDecorator;

/* Group processes a set of given tasks, retying according to a
 * retry check policy. We use a decorated policy that adds a backoff
 * timer between task resubmissions to the executor. Retrying is handled
 * by a retry executor that dispatches retry processing to prevent
 * slowing the main thread in the retrier logic. If any worker fails
 * to pass the retry checks, processing for the group is halted.
 * 
 *  TODO: Refactor to additional classes: especially one for handling retries
 *  and another would be useful to wrap a custom exception transmitted to Group's
 *  main thread from the retrier thread. The exception should contain the
 *  task and group ids.
 */
public class Group<T> {
	private final int groupIndex;
	private final List<Task<T>> tasks = new ArrayList<Task<T>>();
	private final Retrier retryCheckStrategy = new TimedRetrierDecorator(
			new ImmediateCounting());

	private final Map<Future<T>, Integer> futureMap = new HashMap<Future<T>, Integer>();
	private final ExecutorService retryExecutor = Executors
			.newCachedThreadPool();
	private final ExecutorService ecsExecutorService;
	private final ExecutorCompletionService<T> ecs;
	private IllegalStateException retryException;
	private static boolean isRetryExceptionSet = false;

	private Group(GroupBuilder<T> builder) {
		int count = 0;
		for (Callable<T> callable : builder.callables) {
			Task<T> t = new Task<T>(count++, callable);
			tasks.add(t);
		}
		groupIndex = builder.groupIndex;

		ecsExecutorService = Executors
				.newFixedThreadPool(builder.threadPoolSize);
		ecs = new ExecutorCompletionService<T>(ecsExecutorService);
	}

	/*
	 * Group Public API
	 * 
	 * Process group set by submitting the group's work to a threadpool and then
	 * retrying any failed tasks as many times as the retry policy permits.
	 * 
	 * @throws IllegalArgumentException if the Callable is interrupted. This
	 * occurs if retries max out.
	 * 
	 * @returns A list of results in the order the group tasks were provided.
	 */
	public Result<T> processGroup() {
		try {
			submitTasksForProcessing(ecs);
			if (processAsyncTaskResults(ecs)) {
				return getResults();
			} else {
				throw retryException;
			}
		} finally {
			retryExecutor.shutdownNow();
			ecsExecutorService.shutdownNow();
		}
	}

	private void submitTasksForProcessing(CompletionService<T> ecs) {
		for (Task<T> t : tasks) {
			Future<T> future = ecs.submit(t.getCallable());
			futureMap.put(future, t.getTaskId());
		}
	}

	private boolean processAsyncTaskResults(final CompletionService<T> ecs) {
		while (getIncompleteTasks().size() > 0) {
			Future<T> future;
			try {
				// blocks if no jobs are present to take
				future = ecs.take();
				int taskId = futureMap.get(future);

				try {
					T result = future.get();
					Task<T> task = getTask(taskId);
					task.setComplete(true);
					task.setResult(result);
				} catch (ExecutionException e) {
					handleRetries(groupIndex, ecs, taskId);
				} finally {
					futureMap.remove(future);
				}
				// Interrupt when retries are maxed out
			} catch (InterruptedException interrupt) {
				return false;
			}
		}

		return true;
	}

	// handle retries in separate thread to avoid slowing the main thread
	private void handleRetries(final int groupIndex,
			final CompletionService<T> ecs, int taskId)
			throws InterruptedException {
		Runnable retryTask = new RetryWorker(taskId, Thread.currentThread());

		retryExecutor.submit(retryTask);
	}

	private List<Task<T>> getIncompleteTasks() {
		List<Task<T>> incompleteTasks = new ArrayList<Task<T>>();
		for (Task<T> t : tasks) {
			if (!t.isComplete()) {
				incompleteTasks.add(t);
			}
		}
		return incompleteTasks;
	}

	private Task<T> getTask(int taskId) {
		for (Task<T> t : tasks) {
			if (t.getTaskId() == taskId) {
				return t;
			}
		}
		throw new IllegalStateException("Could not find task: " + taskId);
	}

	private Result<T> getResults() {
		Result<T> results = new Result<T>();
		for (Task<T> t : tasks) {
			results.addResult(t.getResult());
		}
		return results;
	}

	private synchronized void setRetryException(IllegalStateException e) {
		if (!isRetryExceptionSet) {
			this.retryException = e;
			isRetryExceptionSet = true;
		}
	}

	private class RetryWorker implements Runnable {
		private final int taskId;
		private final Task<T> task;
		private final Thread mainThread;

		public RetryWorker(int taskId, Thread mainThread) {
			super();
			this.taskId = taskId;
			this.mainThread = mainThread;
			task = getTask(taskId);
		}

		@Override
		public void run() {
			try {
				retryCheckStrategy.retry(groupIndex, taskId);
				futureMap.put(ecs.submit(task.getCallable()), taskId);
			} catch (IllegalStateException e) {
				setRetryException(e);

				// Tell main ecs taker to exit early - retries maxed out
				mainThread.interrupt();
			}
		}
	}

	/*
	 * Builder for Group - this is a basic wrapper that simplifies client
	 * construction
	 */
	public static class GroupBuilder<T> {
		private static AtomicInteger groupIndexCounter = new AtomicInteger(0);
		private final int groupIndex;
		private final List<Callable<T>> callables = new ArrayList<Callable<T>>();
		private int threadPoolSize = 5;

		public GroupBuilder() {
			groupIndex = groupIndexCounter.getAndAdd(1);
		}

		public GroupBuilder<T> addCallable(Callable<T> callable) {
			callables.add(callable);
			return this;
		}

		public GroupBuilder<T> threadPoolSize(int threadPoolSize) {
			this.threadPoolSize = threadPoolSize;
			return this;
		}

		public Group<T> build() {
			return new Group<T>(this);
		}
	}
}
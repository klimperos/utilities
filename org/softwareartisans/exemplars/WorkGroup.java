package org.softwareartisans.exemplars;

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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class WorkGroup<T> {
	private final ExecutorService service;
	private final int retryLimit = 2;
	private final int timeout;
	private final List<Task<T>> tasks = new ArrayList<Task<T>>();
	private int retryCount = 0;

	WorkGroup(ExecutorService service, int timeout, List<Callable<T>> group) {
		this.service = service;
		this.timeout = timeout;

		int count = 0;
		for (Callable<T> callable : group) {
			Task<T> t = new Task<T>(count++, callable);
			tasks.add(t);
		}
	}

	/*
	 * WorkGroup Public API
	 * 
	 * Process group set in the ctor by dispersing the group's work to a
	 * threadpool and then retrying all failed tasks as many times as the retry
	 * policy permits.
	 * 
	 * @param groupIndex the ID for the group's execution order, starting with 0
	 * 
	 * @throws InterruptedException if the callable is interrupted. This
	 * exception is not expected.
	 * 
	 * @returns A list of results in the order the group tasks were provided.
	 */
	public List<T> processGroup(int groupIndex) throws InterruptedException {
		retryCount = 0;

		// Submit all incomplete jobs concurrently
		while (!areResultsComplete(service.invokeAll(getIncompleteCallables(),
				timeout, TimeUnit.MILLISECONDS))) {
			checkRetryPolicy(groupIndex);
		}

		System.out
				.println("Group " + groupIndex + " results = " + getResults());
		return getResults();
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

	private List<Callable<T>> getIncompleteCallables() {
		List<Callable<T>> incompleteCallables = new ArrayList<Callable<T>>();
		List<Task<T>> incompleteTasks = getIncompleteTasks();
		for (Task<T> t : incompleteTasks) {
			if (!t.isComplete()) {
				incompleteCallables.add(t.getCallable());
			}
		}
		return incompleteCallables;
	}

	private List<T> getResults() {
		List<T> results = new ArrayList<T>();
		for (Task<T> t : tasks) {
			results.add(t.getResult());
		}
		return results;
	}

	private boolean areResultsComplete(List<Future<T>> futures)
			throws InterruptedException {
		int count = 0;
		List<Task<T>> incompleteTasks = getIncompleteTasks();
		for (Future<T> future : futures) {
			try {
				T result = future.get();
				Task<T> task = incompleteTasks.get(count++);
				task.setResult(result);
				task.setComplete(true);
			} catch (ExecutionException e) {
				count++;
			}
		}

		return getIncompleteTasks().size() == 0;
	}

	private void checkRetryPolicy(int groupIndex) throws InterruptedException {
		if (retryCount++ == retryLimit) {
			throw new IllegalStateException("Group " + groupIndex
					+ " exceeded retry limit, entire work set cancelled");
		}
	}
}

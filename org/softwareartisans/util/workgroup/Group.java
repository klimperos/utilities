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
import java.util.concurrent.Future;

public class Group<T> {
	private final int taskRetryLimit = 2;
	private final Map<Integer, Integer> retries = new HashMap<Integer, Integer>();
	private final List<Task<T>> tasks = new ArrayList<Task<T>>();

	private Group(GroupBuilder<T> builder) {
		int count = 0;
		for (Callable<T> callable : builder.callables) {
			Task<T> t = new Task<T>(count++, callable);
			tasks.add(t);
		}
	}

	/*
	 * WorkGroup Public API
	 * 
	 * Process group set by submitting the group's work to a threadpool and then
	 * retrying any failed tasks as many times as the retry policy permits.
	 * 
	 * @param s the executor service to use to process each task.
	 * 
	 * @param groupIndex the ID for the group's execution order, starting with 0
	 * 
	 * @throws InterruptedException if the callable is interrupted. This
	 * exception is not expected.
	 * 
	 * @returns A list of results in the order the group tasks were provided.
	 */
	public Result<T> processGroup(ExecutorService s, int groupIndex)
			throws InterruptedException {
		CompletionService<T> ecs = new ExecutorCompletionService<T>(s);
		processAsyncTaskResults(groupIndex, ecs, submitTasksForProcessing(ecs));

		return getResults();
	}

	private Map<Future<T>, Integer> submitTasksForProcessing(
			CompletionService<T> ecs) {
		Map<Future<T>, Integer> futureMap = new HashMap<Future<T>, Integer>();

		for (Task<T> t : tasks) {
			Future<T> future = ecs.submit(t.getCallable());
			futureMap.put(future, t.getTaskId());
		}
		return futureMap;
	}

	private void processAsyncTaskResults(int groupIndex,
			CompletionService<T> ecs, Map<Future<T>, Integer> futureMap)
			throws InterruptedException {
		while (getIncompleteTasks().size() > 0) {
			Future<T> future = ecs.take();
			Integer taskId = futureMap.get(future);

			try {
				T result = future.get();
				Task<T> task = getTask(taskId);
				task.setComplete(true);
				task.setResult(result);
			} catch (ExecutionException e) {
				checkRetryPolicy(groupIndex, taskId);
				futureMap
						.put(ecs.submit(getTask(taskId).getCallable()), taskId);
			}
		}
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

	private void checkRetryPolicy(int groupIndex, int taskId)
			throws InterruptedException {
		Integer taskRetries = 0;
		if (retries.containsKey(taskId)) {
			taskRetries = retries.get(taskId);
		}

		retries.put(taskId, ++taskRetries);

		if (taskRetries > taskRetryLimit) {
			throw new IllegalStateException("Group " + groupIndex
					+ " exceeded retry limit, entire work set cancelled");
		}

	}

	public static class GroupBuilder<T> {
		private final List<Callable<T>> callables = new ArrayList<Callable<T>>();

		public GroupBuilder<T> addCallable(Callable<T> callable) {
			callables.add(callable);
			return this;
		}

		public Group<T> build() {
			return new Group<T>(this);
		}

	}

	public static class GroupsBuilder<T> {
		private final List<Group<T>> groups = new ArrayList<Group<T>>();

		public void addGroup(Group<T> group) {
			groups.add(group);
		}

		public List<Group<T>> build() {
			return groups;
		}
	}
}
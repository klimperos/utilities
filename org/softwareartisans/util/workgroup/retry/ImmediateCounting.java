package org.softwareartisans.util.workgroup.retry;

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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImmediateCounting implements Retrier {
	// TODO: Refactor to ArrayLists
	private static final Map<Integer, Integer> taskRetryCounts = new ConcurrentHashMap<Integer, Integer>();
	private static final Map<Integer, Integer> groupRetryCounts = new ConcurrentHashMap<Integer, Integer>();
	private int globalRetryCount;

	@Value("${spring.taskRetryLimit}")
	private final int taskRetryLimit = 2;

	@Value("${spring.groupRetryLimit}")
	private final int groupRetryLimit = 10;

	@Value("${spring.globalRetryLimit}")
	private final int globalRetryLimit = 20;

	@Override
	public void retry(int groupId, int taskId) {
		checkGlobalRetries(groupId, taskId);
		checkGroupRetries(groupId, taskId);
		checkTaskRetries(groupId, taskId);
	}

	private void checkGlobalRetries(int groupId, int taskId) {
		if (globalRetryCount++ == globalRetryLimit) {
			throw new IllegalStateException(
					"Global retries exceeded limit - Group: " + groupId
							+ " Task: " + taskId);
		}
	}

	private void checkGroupRetries(int groupId, int taskId) {
		if (isRetryLimitReached(groupId, groupRetryLimit, groupRetryCounts)) {
			throw new IllegalStateException("Group: " + groupId
					+ " exceeded retry limit, Task: " + taskId + " failed");
		}
	}

	private void checkTaskRetries(int groupId, int taskId) {
		if (isRetryLimitReached(taskId, taskRetryLimit, taskRetryCounts)) {
			throw new IllegalStateException("Task: " + taskId
					+ " exceeded retry limit, in Group: " + groupId);
		}

	}

	private boolean isRetryLimitReached(int id, int retryLimit,
			Map<Integer, Integer> retryCounts) {
		int retries = 0;

		synchronized (this) {
			if (retryCounts.containsKey(id)) {
				retries = retryCounts.get(id);
			} else {
				retryCounts.put(id, retries);
			}
			retryCounts.put(id, ++retries);
		}

		return retries > retryLimit;
	}
}

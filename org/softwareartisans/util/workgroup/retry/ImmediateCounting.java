package org.softwareartisans.util.workgroup.retry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImmediateCounting implements Retrier {
	// TODO: Change these to ArrayLists: Map is overkill
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

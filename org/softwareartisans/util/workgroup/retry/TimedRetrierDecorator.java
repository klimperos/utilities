package org.softwareartisans.util.workgroup.retry;

import org.springframework.beans.factory.annotation.Value;

public class TimedRetrierDecorator extends RetrierDecorator {
	@Value("${spring.taskRetryWait}")
	private final int retryWait = 1;

	public TimedRetrierDecorator(Retrier decoratedRetrier) {
		super(decoratedRetrier);
	}

	@Override
	public void retry(int groupId, int taskId) {
		super.retry(groupId, taskId);
		try {
			Thread.sleep(retryWait);
		} catch (InterruptedException interrupt) {
		}
	}
}

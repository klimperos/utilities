package org.softwareartisans.util.workgroup.retry;

public abstract class RetrierDecorator implements Retrier {
	private final Retrier decoratedRetrier;

	public RetrierDecorator(Retrier decoratedRetrier) {
		this.decoratedRetrier = decoratedRetrier;
	}

	@Override
	public void retry(int groupId, int taskId) {
		decoratedRetrier.retry(groupId, taskId);
	}
}

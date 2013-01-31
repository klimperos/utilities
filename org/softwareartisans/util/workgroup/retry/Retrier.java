package org.softwareartisans.util.workgroup.retry;

public interface Retrier {
	public void retry(int groupId, int taskId);
}

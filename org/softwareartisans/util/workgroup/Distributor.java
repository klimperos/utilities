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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * Distributor dispatches all work groups and
 * creates the work results list. It shuts down the Executor service
 * upon completion. 
 * 
 * @TODO This class can be readily enhanced to handle a global
 * retry policy and possibly to start multiple groups concurrently.
 * 
 */
public class Distributor<T> {

	private final int threadPoolSize;

	public Distributor(int threadPoolSize) {
		this.threadPoolSize = threadPoolSize;
	}

	public List<Result<T>> doWork(List<Group<Callable<T>>> workGroups) {
		List<Result<T>> workResults = new ArrayList<Result<T>>();
		ExecutorService s = Executors.newFixedThreadPool(threadPoolSize);

		try {
			int groupIndex = 0;
			Processor<T> p;
			for (Group<Callable<T>> workGroup : workGroups) {
				p = Processor.getInstance(workGroup);
				workResults.add(p.processGroup(s, groupIndex++));
			}
		} catch (InterruptedException notExpected) {
			throw new IllegalStateException(notExpected);
		} finally {
			s.shutdown();
		}

		return workResults;
	}
}

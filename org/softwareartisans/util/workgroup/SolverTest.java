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

import java.util.concurrent.Callable;

import org.softwareartisans.util.workgroup.Group.GroupBuilder;
import org.softwareartisans.util.workgroup.Space.SpaceBuilder;

/*
 * Driver to create a set of Workerbee tasks
 * and a couple groups, submit them to the work group distributor
 * and output the result lists.
 */
public class SolverTest {

	public static void main(String[] args) {
		GroupBuilder<Integer> groupBuilder = new GroupBuilder<Integer>();
		for (int i = 0; i < 10; i++) {
			Callable<Integer> workerBee = new WorkerBee(i);
			groupBuilder.addCallable(workerBee);
		}

		SpaceBuilder<Integer> spaceBuilder = new SpaceBuilder<Integer>();
		spaceBuilder.addGroup(groupBuilder.build());
		spaceBuilder.addGroup(groupBuilder.build());
		spaceBuilder.threadPoolSize(10);

		int count = 0;
		for (Result<Integer> groupResult : spaceBuilder.build().solve()) {
			System.out.println("Group " + count++ + ": " + groupResult);
		}
	}

	static class WorkerBee implements Callable<Integer> {
		private final int number;
		private int count = 0;

		WorkerBee(int number) {
			this.number = number;
		}

		@Override
		public Integer call() throws Exception {
			if ((number == 7 || number == 5) && count++ < 2) {
				throw new Exception("Test Exception for thread: " + number);
			} else {
				return number * number;
			}
		}

		@Override
		public String toString() {
			return "Workerbee # " + number;
		}
	}
}
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

public class Space<T> {
	private final List<Group<Callable<T>>> workGroups = new ArrayList<Group<Callable<T>>>();

	// TODO: Configure from Spring
	private final int threads;

	public Space(int threads) {
		this.threads = threads;

	}

	public void addWorkGroup(Group<Callable<T>> group) {
		workGroups.add(group);
	}

	public List<Result<T>> solve() {
		Distributor<T> d = new Distributor<T>(threads);
		return d.doWork(workGroups);
	}
}
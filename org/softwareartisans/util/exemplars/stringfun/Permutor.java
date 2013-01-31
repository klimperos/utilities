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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Permutor {
	public static void main(String[] args) {
		Permutor p = new Permutor();
		int count = 0;
		Set<StringBuilder> permutations = p.getPermutations(args[0]);

		TreeSet<String> results = new TreeSet<String>();
		for (StringBuilder permutation : permutations) {
			results.add(permutation.toString());
		}

		for (String result : results) {
			System.out.println("Permutation = " + result);
			count++;
		}
		System.out.println("Count = " + count);
	}

	Set<StringBuilder> getPermutations(String target) {
		List<Character> chars = new ArrayList<Character>();
		for (int i = 0; i < target.length(); i++) {
			chars.add(target.charAt(i));
		}

		return permute(chars);
	}

	Set<StringBuilder> permute(List<Character> chars) {
		Set<StringBuilder> results = new HashSet<StringBuilder>();

		// Base Case
		if (chars.size() == 1) {
			for (Character c : chars) {
				results.add(new StringBuilder().append(c));
			}
			// Recursive Case: Prepend character to all shorter permutations
		} else {
			List<Character> newChars = new ArrayList<Character>(chars);

			for (Character c : chars) {
				newChars.remove(c);
				Set<StringBuilder> oldPerms = permute(newChars);
				Set<StringBuilder> newPerms = formStrings(c, oldPerms);
				results.addAll(newPerms);
				newChars.add(c);
			}
		}

		return results;
	}

	Set<StringBuilder> formStrings(Character prefix,
			Set<StringBuilder> permutations) {
		Set<StringBuilder> results = new HashSet<StringBuilder>();
		for (StringBuilder permutation : permutations) {
			results.add(permutation.insert(0, prefix));
		}

		return results;
	}
}

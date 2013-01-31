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

/* Permuter: Recursive string permuter
 * 
 * Usage: java Permuter <String-to-be-Permuted>
 */
public class Permuter {
	public static void main(String[] args) {
		Permuter permutor = new Permuter();
		Set<StringBuilder> permutations = permutor.getPermutations(args[0]);

		TreeSet<String> results = new TreeSet<String>();
		for (StringBuilder pe : permutations) {
			results.add(pe.toString());
		}

		int count = 0;
		System.out.println("Permutations:");
		for (String result : results) {
			System.out.println(result);
			count++;
		}
		System.out.println("Permutation Count = " + count);
	}

	Set<StringBuilder> getPermutations(String target) {
		List<Character> chars = new ArrayList<Character>();
		for (int i = 0; i < target.length(); i++) {
			chars.add(target.charAt(i));
		}

		return permute(chars);
	}

	Set<StringBuilder> permute(List<Character> characters) {
		Set<StringBuilder> results = new HashSet<StringBuilder>();

		// Base Case
		if (characters.size() == 1) {
			for (Character c : characters) {
				results.add(new StringBuilder().append(c));
			}
			// Recursive Case: Prepend character to all shorter permutations
		} else {
			List<Character> newCharacters = new ArrayList<Character>(characters);

			for (Character c : characters) {
				newCharacters.remove(c);
				results.addAll(formStrings(c, permute(newCharacters)));
				newCharacters.add(c);
			}
		}

		return results;
	}

	Set<StringBuilder> formStrings(Character prefix,
			Set<StringBuilder> permutations) {
		Set<StringBuilder> results = new HashSet<StringBuilder>();
		for (StringBuilder p : permutations) {
			results.add(p.insert(0, prefix));
		}

		return results;
	}
}

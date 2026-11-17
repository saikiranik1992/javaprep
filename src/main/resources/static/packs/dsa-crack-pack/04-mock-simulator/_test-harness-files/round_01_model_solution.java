/*
 * Mock Simulator Round 1 — TCS NQT Coding Round
 * Reference solutions for Problems 1, 2, 3.
 *
 * These are the implementations the panel would consider strong-pass. They
 * are not the only correct ones, but they hit the complexity and style bar
 * this round is scored on.
 *
 * Open this file ONLY after your 60-minute timer has run out, or after you
 * have written your own attempt in YourSolution.java.
 *
 * NOTE on running this file: the class below is named `Solution` so it
 * matches the contract `01_test_cases.java` expects. Do NOT compile this
 * file together with your own `YourSolution.java` — `javac` will reject
 * two classes called `Solution` in the same compilation unit. To sanity-
 * check the harness against this model solution, run:
 *
 *     javac 02_model_solution.java 01_test_cases.java
 *     java TestCases
 *
 * (and make sure your own YourSolution.java is renamed or moved out of
 * the folder first).
 */

import java.util.*;

class Solution {

    // ---------- Problem 1 — reverseString ----------------------------------

    /**
     * Reverse the array of characters in-place using two pointers from the
     * ends.
     *
     * Time:  O(n) — each index visited once.
     * Space: O(1) — only two pointer variables and one swap temp.
     */
    public void reverseString(char[] s) {
        int left = 0;
        int right = s.length - 1;
        while (left < right) {
            char tmp = s[left];
            s[left] = s[right];
            s[right] = tmp;
            left++;
            right--;
        }
    }

    // ---------- Problem 2 — findMax ----------------------------------------

    /**
     * Single-pass scan tracking the running maximum.
     *
     * IMPORTANT: initialise `best` to nums[0], NOT to 0 or Integer.MIN_VALUE.
     * Initialising to 0 silently corrupts the answer when every element is
     * negative (e.g. {-3, -1, -7} would return 0 instead of -1).
     *
     * Time:  O(n) — one pass.
     * Space: O(1) — one tracker.
     */
    public int findMax(int[] nums) {
        int best = nums[0];
        for (int i = 1; i < nums.length; i++) {
            if (nums[i] > best) {
                best = nums[i];
            }
        }
        return best;
    }

    // ---------- Problem 3 — twoSum -----------------------------------------

    /**
     * Single-pass hash map. For each element x at index i we ask:
     * "have I previously seen the value (target - x)?" If yes, return both
     * indices. If no, record x's index and continue.
     *
     * The key insight: we check `seen.containsKey(complement)` BEFORE
     * inserting x into `seen`. This guarantees we never pair an index with
     * itself, which matters for inputs like {3, 3} with target 6.
     *
     * Time:  O(n) — one pass, O(1) hash operations.
     * Space: O(n) — the hash map can hold up to n entries.
     */
    public int[] twoSum(int[] nums, int target) {
        Map<Integer, Integer> seen = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            int complement = target - nums[i];
            if (seen.containsKey(complement)) {
                return new int[] {seen.get(complement), i};
            }
            seen.put(nums[i], i);
        }
        // The problem guarantees exactly one solution, so this line is
        // unreachable in spec. Returning an empty array keeps the return
        // type honest if the spec is ever loosened.
        return new int[0];
    }
}

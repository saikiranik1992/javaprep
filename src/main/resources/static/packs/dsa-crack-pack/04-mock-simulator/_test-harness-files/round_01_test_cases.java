/*
 * Mock Simulator Round 1 — TCS NQT Coding Round
 * Auto-grader for YourSolution.java
 *
 * Usage:
 *     1. Write your three methods inside `class Solution` in `YourSolution.java`
 *        (same folder as this file). Use this skeleton:
 *
 *            class Solution {
 *                public void reverseString(char[] s) { ... }
 *                public int findMax(int[] nums) { ... }
 *                public int[] twoSum(int[] nums, int target) { ... }
 *            }
 *
 *     2. From this folder, compile and run:
 *            javac YourSolution.java 01_test_cases.java
 *            java TestCases
 *
 *     3. The harness reports pass/fail for each of 15 hidden cases plus a
 *        final score.
 *
 * Note: this file declares package-private classes only, so the filename
 * `01_test_cases.java` is legal even though it starts with a digit. The
 * runtime entry point is the class `TestCases`.
 */

import java.util.*;

class TestCases {

    public static void main(String[] args) {
        Solution sol = new Solution();

        int p1 = runProblem1(sol);
        int p2 = runProblem2(sol);
        int p3 = runProblem3(sol);
        int total = p1 + p2 + p3;

        System.out.println();
        System.out.println("========================================");
        System.out.printf("  Problem 1 score: %d/5%n", p1);
        System.out.printf("  Problem 2 score: %d/5%n", p2);
        System.out.printf("  Problem 3 score: %d/5%n", p3);
        System.out.printf("  TOTAL:           %d/15%n", total);
        System.out.println("========================================");

        if (total == 15) {
            System.out.println();
            System.out.println("Clean sweep. Now grade yourself on style + clarification habit using 04_scoring_rubric.md.");
        } else if (total >= 12) {
            System.out.println();
            System.out.println("Strong pass. Open 03_walkthrough.md and diff your approach against the model.");
        } else if (total >= 8) {
            System.out.println();
            System.out.println("Mixed pass. Likely one method half-working. Walkthrough will show you where.");
        } else {
            System.out.println();
            System.out.println("Below the bar for TCS NQT today. Re-read the patterns and try again in 48 hours.");
        }
    }

    // ---------- Problem 1 — reverseString ----------------------------------

    static int runProblem1(Solution sol) {
        System.out.println();
        System.out.println("--- Problem 1 — reverseString ---");
        int passed = 0;
        passed += check("P1.1 canonical 'hello'",
                testReverse(sol, new char[] {'h', 'e', 'l', 'l', 'o'},
                                  new char[] {'o', 'l', 'l', 'e', 'h'}));
        passed += check("P1.2 single element",
                testReverse(sol, new char[] {'a'},
                                  new char[] {'a'}));
        passed += check("P1.3 two elements",
                testReverse(sol, new char[] {'a', 'b'},
                                  new char[] {'b', 'a'}));
        passed += check("P1.4 palindrome 'abba'",
                testReverse(sol, new char[] {'a', 'b', 'b', 'a'},
                                  new char[] {'a', 'b', 'b', 'a'}));
        passed += check("P1.5 long mixed case",
                testReverse(sol, "Hello World!".toCharArray(),
                                  "!dlroW olleH".toCharArray()));
        return passed;
    }

    static boolean testReverse(Solution sol, char[] input, char[] expected) {
        char[] work = input.clone();
        try {
            sol.reverseString(work);
        } catch (Exception e) {
            System.out.printf("    (raised %s: %s)%n",
                    e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
        return Arrays.equals(work, expected);
    }

    // ---------- Problem 2 — findMax ----------------------------------------

    static int runProblem2(Solution sol) {
        System.out.println();
        System.out.println("--- Problem 2 — findMax ---");
        int passed = 0;
        passed += check("P2.1 canonical mixed",
                testMax(sol, new int[] {3, 1, 4, 1, 5, 9, 2, 6}, 9));
        passed += check("P2.2 single negative",
                testMax(sol, new int[] {-5}, -5));
        passed += check("P2.3 all negatives",
                testMax(sol, new int[] {-3, -1, -7, -2}, -1));
        passed += check("P2.4 max at start",
                testMax(sol, new int[] {100, 1, 2, 3}, 100));
        passed += check("P2.5 max at end",
                testMax(sol, new int[] {1, 2, 3, 100}, 100));
        return passed;
    }

    static boolean testMax(Solution sol, int[] nums, int expected) {
        try {
            return sol.findMax(nums.clone()) == expected;
        } catch (Exception e) {
            System.out.printf("    (raised %s: %s)%n",
                    e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    // ---------- Problem 3 — twoSum -----------------------------------------

    static int runProblem3(Solution sol) {
        System.out.println();
        System.out.println("--- Problem 3 — twoSum ---");
        int passed = 0;
        passed += check("P3.1 canonical",
                testTwoSum(sol, new int[] {2, 7, 11, 15}, 9,
                        new int[][] {{0, 1}}));
        passed += check("P3.2 no index reuse",
                testTwoSum(sol, new int[] {3, 2, 4}, 6,
                        new int[][] {{1, 2}}));
        passed += check("P3.3 duplicate values",
                testTwoSum(sol, new int[] {3, 3}, 6,
                        new int[][] {{0, 1}}));
        passed += check("P3.4 negatives + zero",
                testTwoSum(sol, new int[] {-3, 4, 3, 90}, 0,
                        new int[][] {{0, 2}}));
        passed += check("P3.5 large gap",
                testTwoSum(sol, new int[] {1, 5, 8, 12, 19, 23}, 31,
                        new int[][] {{3, 4}}));
        return passed;
    }

    static boolean testTwoSum(Solution sol, int[] nums, int target,
                              int[][] validPairs) {
        int[] raw;
        try {
            raw = sol.twoSum(nums.clone(), target);
        } catch (Exception e) {
            System.out.printf("    (raised %s: %s)%n",
                    e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
        if (raw == null || raw.length != 2) {
            return false;
        }
        // twoSum may return either ordering of the pair; normalise to sorted
        // tuple and compare against the set of acceptable answers.
        int[] got = new int[] {Math.min(raw[0], raw[1]),
                               Math.max(raw[0], raw[1])};
        for (int[] pair : validPairs) {
            int[] norm = new int[] {Math.min(pair[0], pair[1]),
                                    Math.max(pair[0], pair[1])};
            if (got[0] == norm[0] && got[1] == norm[1]) {
                return true;
            }
        }
        return false;
    }

    // ---------- Tiny helper -----------------------------------------------

    static int check(String label, boolean ok) {
        if (ok) {
            System.out.printf("  [PASS] %s%n", label);
            return 1;
        } else {
            System.out.printf("  [FAIL] %s%n", label);
            return 0;
        }
    }
}

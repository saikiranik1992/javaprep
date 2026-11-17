/*
 * Round 2 - Flipkart On-Campus SDE-1 . Test Harness (Java 17+)
 *
 * Compile and run:
 *     javac 01_test_cases.java 02_model_solution.java
 *     java TestHarness
 *
 * The harness loads ModelSolution by default. To grade your own
 * attempt, copy your file as YourSolution.java in this folder with
 * a class named YourSolution that extends ModelSolution and
 * overrides the two methods. Then flip SOLUTION_SOURCE below to
 * "your" and re-run. (Extending ModelSolution lets the harness type
 * the field as ModelSolution while still calling your overrides.)
 *
 * 15 tests total:
 *   - 10 for maxProduct        (the main solve)
 *   -  5 for maxProductSubarray (the follow-up)
 *
 * Auto-fail conditions on the rubric apply to:
 *   - {-2, 3, -4}  must return 24
 *   - {-3}         must return -3
 * If either fails, your Correctness score is 0 regardless of the
 * other test outcomes - see 04_scoring_rubric.md.
 */

import java.util.Arrays;

// Note: package-private so the file name can be 01_test_cases.java
// rather than TestHarness.java. The class still has a `main` method
// so `java TestHarness` runs it after `javac` produces TestHarness.class.
class TestHarness {

    // Toggle this to "your" once you have written YourSolution.java
    // with a class `YourSolution extends ModelSolution` that
    // overrides both methods. The harness will pick it up via
    // reflection and run all 15 tests against your code.
    private static final String SOLUTION_SOURCE = "model";

    // -------- main solve: maxProduct -----------------------------

    private static final MainCase[] MAIN_CASES = new MainCase[] {
        new MainCase(
            "ex1: textbook positive-positive-negative-positive",
            new int[] {2, 3, -2, 4}, 6),
        new MainCase(
            "ex2: zero between two negatives - zero wins",
            new int[] {-2, 0, -1}, 0),
        new MainCase(
            "ex3: two negatives cancel across full array  [AUTO-FAIL CASE]",
            new int[] {-2, 3, -4}, 24),
        new MainCase(
            "ex4: double negative times positive",
            new int[] {-2, -3, 7}, 42),
        new MainCase(
            "ex5: leading zero - skip and take the positive",
            new int[] {0, 2}, 2),
        new MainCase(
            "edge: single positive element",
            new int[] {5}, 5),
        new MainCase(
            "edge: single negative element  [AUTO-FAIL CASE]",
            new int[] {-3}, -3),
        new MainCase(
            "edge: all zeros",
            new int[] {0, 0, 0}, 0),
        new MainCase(
            "edge: all negatives, odd count - best is 4-window",
            new int[] {-1, -2, -3, -4, -5}, 120),
        new MainCase(
            "edge: mix where best window is a strict subarray",
            new int[] {2, -5, -2, -4, 3}, 24),
    };

    // -------- follow-up: maxProductSubarray ----------------------

    private static final FollowupCase[] FOLLOWUP_CASES = new FollowupCase[] {
        new FollowupCase(
            "subarray ex1",
            new int[] {2, 3, -2, 4}, 6),
        new FollowupCase(
            "subarray ex3 - full array two-negative flip",
            new int[] {-2, 3, -4}, 24),
        new FollowupCase(
            "subarray ex4 - full array",
            new int[] {-2, -3, 7}, 42),
        new FollowupCase(
            "subarray edge - single element",
            new int[] {-3}, -3),
        new FollowupCase(
            "subarray edge - skip zero in middle, take three-window",
            new int[] {2, -5, -2, -4, 3}, 24),
    };

    // -------- runner --------------------------------------------

    public static void main(String[] args) {
        // Pick the implementation under test. The harness keeps a
        // single reference so we never accidentally test one class
        // against tests written for another.
        ModelSolution sol;
        String sourceLabel;
        if ("your".equals(SOLUTION_SOURCE)) {
            // Compilation will fail loudly if YourSolution does not
            // exist or does not expose the two required methods.
            try {
                Class<?> cls = Class.forName("YourSolution");
                sol = (ModelSolution) cls.getDeclaredConstructor()
                        .newInstance();
                sourceLabel = "YourSolution.java";
            } catch (Exception e) {
                System.err.println(
                    "[harness] Could not load YourSolution. "
                    + "Falling back to ModelSolution. Reason: "
                    + e.getMessage());
                sol = new ModelSolution();
                sourceLabel = "02_model_solution.java (fallback)";
            }
        } else {
            sol = new ModelSolution();
            sourceLabel = "02_model_solution.java";
        }

        System.out.println("Source under test: " + sourceLabel);

        int mainPassed = runMain(sol);
        int followupPassed = runFollowup(sol);

        int total = MAIN_CASES.length + FOLLOWUP_CASES.length;
        int score = mainPassed + followupPassed;

        System.out.println();
        System.out.println("----- score: " + score + " / " + total + " -----");

        if (score == total) {
            System.out.println(
                "All green. Cross-check against 04_scoring_rubric.md "
                + "for clarification + style scoring.");
        } else if (mainPassed < MAIN_CASES.length) {
            System.out.println(
                "Main solve failing. Re-derive the curMax / curMin "
                + "invariant before re-running.");
        } else {
            System.out.println(
                "Main solve clean, follow-up failing. Check your "
                + "start-index tracking for the negative-flip branch.");
        }

        System.exit(score == total ? 0 : 1);
    }

    private static int runMain(ModelSolution sol) {
        int passed = 0;
        System.out.println();
        System.out.println(
            "[main] maxProduct - " + MAIN_CASES.length + " tests");
        System.out.println();

        for (MainCase tc : MAIN_CASES) {
            int got = sol.maxProduct(tc.nums.clone());
            boolean ok = got == tc.expected;
            String flag = ok ? "PASS" : "FAIL";
            System.out.println("  " + flag + "  " + tc.label);
            if (!ok) {
                System.out.println(
                    "        nums     = " + Arrays.toString(tc.nums));
                System.out.println("        expected = " + tc.expected);
                System.out.println("        got      = " + got);
            }
            if (ok) {
                passed++;
            }
        }
        return passed;
    }

    private static int runFollowup(ModelSolution sol) {
        int passed = 0;
        System.out.println();
        System.out.println(
            "[follow-up] maxProductSubarray - "
            + FOLLOWUP_CASES.length + " tests");
        System.out.println();

        for (FollowupCase tc : FOLLOWUP_CASES) {
            int[] sub = sol.maxProductSubarray(tc.nums.clone());

            // Validate: subarray must be a contiguous slice of nums
            // whose product equals expectedProduct. Guard against a
            // null return so we report cleanly instead of NPE.
            boolean ok;
            int product = 1;
            if (sub == null) {
                ok = false;
            } else {
                for (int v : sub) {
                    product *= v;
                }
                ok = product == tc.expectedProduct
                        && isContiguousSlice(tc.nums, sub);
            }

            String flag = ok ? "PASS" : "FAIL";
            System.out.println("  " + flag + "  " + tc.label);
            if (!ok) {
                System.out.println(
                    "        nums             = "
                    + Arrays.toString(tc.nums));
                System.out.println(
                    "        expected product = " + tc.expectedProduct);
                System.out.println(
                    "        got subarray     = "
                    + (sub == null ? "null" : Arrays.toString(sub))
                    + "  (product = " + product + ")");
            }
            if (ok) {
                passed++;
            }
        }
        return passed;
    }

    /**
     * True if sub is a contiguous slice of nums. Empty slice is
     * treated as not a valid answer because the problem requires a
     * non-empty subarray.
     */
    private static boolean isContiguousSlice(int[] nums, int[] sub) {
        if (sub.length == 0 || sub.length > nums.length) {
            return false;
        }
        outer:
        for (int start = 0; start + sub.length <= nums.length; start++) {
            for (int j = 0; j < sub.length; j++) {
                if (nums[start + j] != sub[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    // -------- test case records -----------------------------------

    private static final class MainCase {
        final String label;
        final int[] nums;
        final int expected;

        MainCase(String label, int[] nums, int expected) {
            this.label = label;
            this.nums = nums;
            this.expected = expected;
        }
    }

    private static final class FollowupCase {
        final String label;
        final int[] nums;
        final int expectedProduct;

        FollowupCase(String label, int[] nums, int expectedProduct) {
            this.label = label;
            this.nums = nums;
            this.expectedProduct = expectedProduct;
        }
    }
}

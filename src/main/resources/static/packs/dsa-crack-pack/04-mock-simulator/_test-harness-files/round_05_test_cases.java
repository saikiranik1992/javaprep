/*
 * Mock Simulator Round 5 — Auto-grader (20 test cases, weighted)
 *
 * How to use:
 *   1. Write your implementation in `YourSolution.java` (a sibling file
 *      in this folder). It must expose a top-level class `RateLimiter`
 *      with a single public method:
 *
 *          public boolean isAllowed(String userId, double nowSeconds)
 *
 *   2. Compile and run from this folder:
 *
 *          javac YourSolution.java 01_test_cases.java
 *          java TestCases
 *
 *      If `YourSolution.java` is missing, fall back to the model
 *      solution:
 *
 *          javac 02_model_solution.java 01_test_cases.java
 *          java TestCases
 *
 * The grader injects `nowSeconds` explicitly into each call — we never
 * use wall-clock time. That is on purpose. A senior implementation must
 * not call `System.nanoTime()` or `System.currentTimeMillis()` inside
 * isAllowed; it should accept the timestamp as a parameter so it is
 * testable and so it composes cleanly under distributed clock sources.
 *
 * The class in this file is `TestCases`. Because Java requires the
 * public class name to match the filename and identifiers cannot start
 * with a digit, this file cannot use `public class TestCases`.
 * `TestCases` is package-private — `javac` accepts that as long as no
 * `public` class is declared. The compiled `TestCases.class` ends up
 * in the default package alongside `RateLimiter.class`, which is how
 * `java TestCases` finds both.
 */

import java.util.ArrayList;
import java.util.List;

class TestCases {

    // Each test: name, weight, runnable (returns Result).
    interface TestFn {
        Result run();
    }

    static class Result {
        final boolean passed;
        final String detail;
        Result(boolean passed, String detail) {
            this.passed = passed;
            this.detail = detail;
        }
        static Result pass(String detail) { return new Result(true, detail); }
        static Result fail(String detail) { return new Result(false, detail); }
    }

    static class TestCase {
        final String name;
        final double weight;
        final TestFn fn;
        TestCase(String name, double weight, TestFn fn) {
            this.name = name;
            this.weight = weight;
            this.fn = fn;
        }
    }

    // --------------------------------------------------------------
    // Test bodies. Each constructs a fresh RateLimiter and exercises it.
    // --------------------------------------------------------------

    static Result t1FirstTenAllowed() {
        RateLimiter rl = new RateLimiter();
        for (int i = 0; i < 10; i++) {
            if (!rl.isAllowed("alice", 0.0 + i * 0.01)) {
                return Result.fail("request " + (i + 1) + " unexpectedly rejected");
            }
        }
        return Result.pass("10/10 allowed in first second");
    }

    static Result t2EleventhRejected() {
        RateLimiter rl = new RateLimiter();
        for (int i = 0; i < 10; i++) {
            rl.isAllowed("alice", 0.0 + i * 0.01);
        }
        boolean ok = rl.isAllowed("alice", 0.5);
        if (ok) {
            return Result.fail("11th request in same second was allowed");
        }
        return Result.pass("11th correctly rejected");
    }

    static Result t3IndependentUsers() {
        RateLimiter rl = new RateLimiter();
        for (int i = 0; i < 10; i++) {
            rl.isAllowed("alice", 0.0 + i * 0.01);
        }
        if (!rl.isAllowed("bob", 0.5)) {
            return Result.fail("bob blocked by alice's quota");
        }
        return Result.pass("users are isolated");
    }

    static Result t4WindowSlidesAfterOneSecond() {
        RateLimiter rl = new RateLimiter();
        for (int i = 0; i < 10; i++) {
            rl.isAllowed("alice", 0.0 + i * 0.05);   // t = 0.00..0.45
        }
        // at t=1.01, the t=0.00 entry is older than 1s, so capacity = 1 freed
        if (!rl.isAllowed("alice", 1.01)) {
            return Result.fail("window did not slide after 1 second");
        }
        return Result.pass("window slides correctly");
    }

    static Result t5HundredInTenSeconds() {
        RateLimiter rl = new RateLimiter();
        // 100 requests at 0.1001s spacing — total span 99 * 0.1001 = 9.9099s,
        // comfortably inside the 10s window. The 0.0001 padding over 0.1
        // is deliberate: at exact 0.1s spacing, the 10th-oldest entry sits
        // right on the eviction boundary and IEEE-754 drift can make
        // `prev > cutoff` evaluate true, leaving the deque at 10 and
        // rejecting the 11th. The padding gives a true ~1.001s gap to
        // the 10th-back, evicting it cleanly.
        // IMPORTANT: this is the kind of off-by-epsilon a senior is
        // expected to notice in production — see walkthrough minute 55.
        for (int k = 0; k < 100; k++) {
            double t = k * 0.1001;
            if (!rl.isAllowed("alice", t)) {
                return Result.fail("req " + (k + 1) + " rejected at t=" + t);
            }
        }
        return Result.pass("100 requests over 10s all allowed");
    }

    static Result t6HundredFirstThenRejected() {
        RateLimiter rl = new RateLimiter();
        // Fill up the 10-second budget with 100 cleanly-spaced requests.
        for (int k = 0; k < 100; k++) {
            rl.isAllowed("alice", k * 0.1001);
        }
        // at t = 9.92 we still have 100 in the last 10s (oldest at 0.0
        // is within (-0.08, 9.92]) -> reject
        if (rl.isAllowed("alice", 9.92)) {
            return Result.fail("101st in 10s was allowed");
        }
        return Result.pass("101st correctly rejected by 10s cap");
    }

    static Result t7OneSecCapIndependentOfTenSec() {
        // Burst 10 in the first 0.1s. Spread 50 more across the next 9s.
        // At second 1.5 we are within 10s cap (60 used / 100). But if we
        // try an 11th in any single 1-second window we must still be
        // rejected by the 1-second cap.
        RateLimiter rl = new RateLimiter();
        for (int i = 0; i < 10; i++) {
            rl.isAllowed("alice", 0.0 + i * 0.01);
        }
        // Now between t=1.0 and t=1.5 the 10s window still holds those 10.
        // Fire 10 more between t=1.1 and t=1.5 — all should be allowed.
        for (int i = 0; i < 10; i++) {
            if (!rl.isAllowed("alice", 1.1 + i * 0.03)) {
                return Result.fail("req " + (i + 1) + " in second second rejected");
            }
        }
        // 11th in 1.1..1.5 window — rejected by 1-sec cap
        if (rl.isAllowed("alice", 1.5)) {
            return Result.fail("11th in 1s window allowed");
        }
        return Result.pass("1s cap enforced independently of 10s budget");
    }

    static Result t8RejectedDoesNotConsumeQuota() {
        RateLimiter rl = new RateLimiter();
        for (int i = 0; i < 10; i++) {
            rl.isAllowed("alice", 0.0 + i * 0.01);
        }
        // spam 50 rejected requests
        for (int i = 0; i < 50; i++) {
            rl.isAllowed("alice", 0.5 + i * 0.001);
        }
        // at t=1.5 the 1-sec window is clear; alice should still get her
        // next 10 allowed because the rejections did not eat into her
        // 10-second budget.
        for (int i = 0; i < 10; i++) {
            if (!rl.isAllowed("alice", 1.5 + i * 0.01)) {
                return Result.fail("req " + (i + 1) + " after rejections was blocked");
            }
        }
        return Result.pass("rejected requests do not consume quota");
    }

    static Result t9ZeroTimestamp() {
        RateLimiter rl = new RateLimiter();
        if (!rl.isAllowed("alice", 0.0)) {
            return Result.fail("request at t=0 rejected");
        }
        return Result.pass("t=0 works as a valid timestamp");
    }

    static Result t10ExactBoundaryOneSecond() {
        // A request at t=0.0 and another at t=1.0 — is the t=0.0 one
        // inside the window (cutoff..now] or outside?
        // Spec: window is (now - 1.0, now] — half-open on the older side.
        // So a t=0.0 entry at now=1.0 is OUTSIDE the window (cutoff is
        // exactly 0.0, and the comparison is <= cutoff for eviction).
        RateLimiter rl = new RateLimiter();
        for (int i = 0; i < 10; i++) {
            rl.isAllowed("alice", 0.0 + i * 0.001);   // ten at ~t=0
        }
        // at t=1.0, the t=0.0 entry is at the boundary -> evicted.
        // Capacity freed, so this should be allowed.
        if (!rl.isAllowed("alice", 1.0)) {
            return Result.fail("boundary at exactly now-1.0 was not evicted");
        }
        return Result.pass("exact boundary at now-1.0 correctly excluded");
    }

    static Result t11ExactBoundaryTenSeconds() {
        RateLimiter rl = new RateLimiter();
        for (int sec = 0; sec < 10; sec++) {
            for (int i = 0; i < 10; i++) {
                rl.isAllowed("alice", sec + i * 0.09);
            }
        }
        // at t=10.0, the t=0.0 entry is at the 10s boundary -> evicted.
        // That frees one slot in the 10s cap.
        if (!rl.isAllowed("alice", 10.0)) {
            return Result.fail("10s boundary was not evicted");
        }
        return Result.pass("exact boundary at now-10.0 correctly excluded");
    }

    static Result t12TwoUsersAlternating() {
        RateLimiter rl = new RateLimiter();
        // both should get exactly 10 each because each is capped at 10/sec
        // individually. 15 attempts in 0.75s is > 10. The 11th in the same
        // second should fail. Check that both succeed for the first 10
        // and fail for the next 5.
        int aOk = 0, bOk = 0;
        for (int i = 0; i < 15; i++) {
            double t = i * 0.05;
            if (rl.isAllowed("alice", t)) aOk++;
            if (rl.isAllowed("bob", t)) bOk++;
        }
        if (aOk != 10 || bOk != 10) {
            return Result.fail("alice=" + aOk + ", bob=" + bOk + " (expected 10, 10)");
        }
        return Result.pass("interleaved users each get their own 10");
    }

    static Result t13ManyShortWindows() {
        // 50 requests over ~5 seconds at 0.1001s spacing (padded to dodge
        // float-precision drift on the eviction boundary — see t5).
        RateLimiter rl = new RateLimiter();
        for (int k = 0; k < 50; k++) {
            double t = k * 0.1001;
            if (!rl.isAllowed("alice", t)) {
                return Result.fail("failed at k=" + k + ", t=" + t);
            }
        }
        return Result.pass("50 requests across 5 seconds all allowed");
    }

    static Result t14SparseUserOverLongTime() {
        // One request every 2 seconds for 30 seconds — 15 requests.
        // All should be allowed (well under both caps).
        RateLimiter rl = new RateLimiter();
        for (int i = 0; i < 15; i++) {
            if (!rl.isAllowed("alice", i * 2.0)) {
                return Result.fail("sparse request " + i + " rejected");
            }
        }
        return Result.pass("sparse traffic always allowed");
    }

    static Result t15MemoryBoundLowActive() {
        // 100 users, 5 requests each. Limiter must not crash or take
        // silly time. We do not measure memory in the unit test (CI
        // variance), but we exercise the path.
        RateLimiter rl = new RateLimiter();
        int totalOk = 0;
        for (int u = 0; u < 100; u++) {
            for (int i = 0; i < 5; i++) {
                if (rl.isAllowed("user_" + u, i * 0.1)) {
                    totalOk++;
                }
            }
        }
        if (totalOk != 500) {
            return Result.fail("expected 500 allowed, got " + totalOk);
        }
        return Result.pass("100 users x 5 reqs handled cleanly");
    }

    static Result t16MemoryBoundHighCardinality() {
        // 1000 unique users, 1 request each. Stress the map.
        RateLimiter rl = new RateLimiter();
        for (int u = 0; u < 1000; u++) {
            if (!rl.isAllowed("u" + u, u * 0.001)) {
                return Result.fail("unique user " + u + " unexpectedly rejected");
            }
        }
        return Result.pass("1000 unique users handled");
    }

    static Result t17BurstThenSteady() {
        RateLimiter rl = new RateLimiter();
        // Burst 10 at t=0
        for (int i = 0; i < 10; i++) {
            rl.isAllowed("alice", 0.0);
        }
        // 11th at t=0 rejected
        if (rl.isAllowed("alice", 0.0)) {
            return Result.fail("11th in same instant allowed");
        }
        // Steady 1 req/sec from t=1.1 to t=10.1 — 10 more
        // All allowed; total used so far = 20.
        for (int sec = 1; sec <= 10; sec++) {
            if (!rl.isAllowed("alice", sec + 0.1)) {
                return Result.fail("steady request at t=" + (sec + 0.1) + " rejected");
            }
        }
        return Result.pass("burst-then-steady mixes correctly");
    }

    static Result t18UserQuotaResetsAfterTenSeconds() {
        RateLimiter rl = new RateLimiter();
        // Use full 100-request 10s budget in the first 10s.
        for (int sec = 0; sec < 10; sec++) {
            for (int i = 0; i < 10; i++) {
                rl.isAllowed("alice", sec + i * 0.09);
            }
        }
        // At t=10.01, the t=0.0 batch falls outside both windows.
        // Should be allowed.
        if (!rl.isAllowed("alice", 10.01)) {
            return Result.fail("user not allowed after 10s of quiet");
        }
        return Result.pass("10s budget rolls forward correctly");
    }

    static Result t19ExactCapacityAtBoundary() {
        // 9 requests in first second, then 1 more at t=0.99 — that is
        // the 10th, within window, must be allowed.
        RateLimiter rl = new RateLimiter();
        for (int i = 0; i < 9; i++) {
            rl.isAllowed("alice", i * 0.1);
        }
        if (!rl.isAllowed("alice", 0.99)) {
            return Result.fail("10th request at t=0.99 rejected");
        }
        // Next request at t=0.999 must be rejected (11 in same window)
        if (rl.isAllowed("alice", 0.999)) {
            return Result.fail("11th request at t=0.999 allowed");
        }
        return Result.pass("exact-capacity boundary respected");
    }

    static Result t20HugeRun() {
        // Stress run: alice tries 1 request every 50ms for 12 seconds.
        // Expected: cap of 10 per second for 10 seconds = 100 allowed
        // in the first 10s window, then we are 10s-budget-bound. After
        // that, the oldest requests start to fall out of the 10s window
        // and capacity returns at the same rate it left.
        RateLimiter rl = new RateLimiter();
        int allowed = 0;
        // 12 seconds * 20 requests per second = 240 total attempts at 50ms
        for (int step = 0; step < 240; step++) {
            double t = step * 0.05;
            if (rl.isAllowed("alice", t)) {
                allowed++;
            }
        }
        // Cannot exceed 10/sec average over a 12-second window where each
        // second has 20 attempts -> 10 allowed per second -> 120 max.
        // But the 10s cap kicks in too — after the first 10 seconds we've
        // used 100, and only as old slots fall out do new ones become
        // available. The 1-sec cap is the binding constraint for the
        // first ~10s, the 10-sec cap binds after.
        // Hand-computed expected: at any instant after t=10, the 10s
        // window holds the most recent 100 allowed timestamps. So
        // roughly 100 are allowed in the first 10s, then 1 every 0.1s
        // after that as old ones expire — but we cannot exceed the
        // 1-sec cap of 10 either. Net: 120 allowed.
        int expected = 120;
        if (allowed != expected) {
            return Result.fail("allowed=" + allowed + ", expected=" + expected);
        }
        return Result.pass("long run: " + allowed + " allowed (matches expected)");
    }

    // --------------------------------------------------------------
    // Harness
    // --------------------------------------------------------------

    static List<TestCase> buildTests() {
        List<TestCase> tests = new ArrayList<>();
        tests.add(new TestCase("first 10 in 1s allowed",                1.0, TestCases::t1FirstTenAllowed));
        tests.add(new TestCase("11th in same second rejected",          1.0, TestCases::t2EleventhRejected));
        tests.add(new TestCase("users isolated from each other",        1.0, TestCases::t3IndependentUsers));
        tests.add(new TestCase("1s window slides after time passes",    1.0, TestCases::t4WindowSlidesAfterOneSecond));
        tests.add(new TestCase("100 requests over 10s allowed",         1.0, TestCases::t5HundredInTenSeconds));
        tests.add(new TestCase("101st request in 10s rejected",         1.0, TestCases::t6HundredFirstThenRejected));
        tests.add(new TestCase("1s cap enforced inside 10s window",     1.5, TestCases::t7OneSecCapIndependentOfTenSec));
        tests.add(new TestCase("rejected requests don't consume quota", 1.5, TestCases::t8RejectedDoesNotConsumeQuota));
        tests.add(new TestCase("t=0 is a valid timestamp",              0.5, TestCases::t9ZeroTimestamp));
        tests.add(new TestCase("1s boundary at exactly now-1.0",        1.5, TestCases::t10ExactBoundaryOneSecond));
        tests.add(new TestCase("10s boundary at exactly now-10.0",      1.5, TestCases::t11ExactBoundaryTenSeconds));
        tests.add(new TestCase("two users alternating requests",        1.0, TestCases::t12TwoUsersAlternating));
        tests.add(new TestCase("50 reqs across 5 seconds clean",        1.0, TestCases::t13ManyShortWindows));
        tests.add(new TestCase("sparse user over 30 seconds",           0.5, TestCases::t14SparseUserOverLongTime));
        tests.add(new TestCase("100 users x 5 reqs each",               1.0, TestCases::t15MemoryBoundLowActive));
        tests.add(new TestCase("1000 unique users",                     1.0, TestCases::t16MemoryBoundHighCardinality));
        tests.add(new TestCase("burst then steady mix",                 1.0, TestCases::t17BurstThenSteady));
        tests.add(new TestCase("10s budget rolls forward",              1.0, TestCases::t18UserQuotaResetsAfterTenSeconds));
        tests.add(new TestCase("exact-capacity at 1s boundary",         1.5, TestCases::t19ExactCapacityAtBoundary));
        tests.add(new TestCase("12-second stress run",                  2.0, TestCases::t20HugeRun));
        return tests;
    }

    public static void main(String[] args) {
        System.out.println("Grader: running RateLimiter against 20 weighted test cases\n");
        List<TestCase> tests = buildTests();
        double earned = 0.0;
        double total = 0.0;
        List<String> failures = new ArrayList<>();
        for (TestCase tc : tests) {
            total += tc.weight;
            Result r;
            try {
                r = tc.fn.run();
            } catch (Exception exc) {
                r = Result.fail("raised " + exc.getClass().getSimpleName() + ": " + exc.getMessage());
            }
            String marker = r.passed ? "PASS" : "FAIL";
            System.out.printf("  [%s] (%.1f) %s  --  %s%n", marker, tc.weight, tc.name, r.detail);
            if (r.passed) {
                earned += tc.weight;
            } else {
                failures.add(tc.name);
            }
        }
        System.out.println();
        System.out.printf("Score: %.1f / %.1f%n", earned, total);
        double pct = total == 0 ? 0 : (earned / total) * 100;
        System.out.printf("Percentage: %.1f%%%n", pct);
        if (!failures.isEmpty()) {
            System.out.println("Failed cases (" + failures.size() + "):");
            for (String name : failures) {
                System.out.println("  - " + name);
            }
        }
        System.out.println();
        if (pct >= 90) {
            System.out.println("Verdict: Senior SDE-2 ready on correctness. Now check 04_scoring_rubric.md for the other 4 dimensions.");
        } else if (pct >= 70) {
            System.out.println("Verdict: Solid base, missing senior signal on edge cases. Review failed tests.");
        } else {
            System.out.println("Verdict: Correctness gaps. Reread 03_walkthrough.md before retrying.");
        }
        System.exit(pct >= 90 ? 0 : 1);
    }
}

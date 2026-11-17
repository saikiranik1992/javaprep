/*
 * Mock Simulator Round 3 — Razorpay SDE-1 auto-grader.
 *
 * Compile and run from this folder:
 *
 *   javac 01_test_cases.java PaymentFunnel.java
 *   java TestCases
 *
 * Java requires the file name to match the public class name. Write your
 * attempt in PaymentFunnel.java in this folder. If you have not written one
 * yet and only want to sanity-check the harness, copy the model solution:
 *
 *   cp 02_model_solution.java PaymentFunnel.java
 *
 * Then change the line `class PaymentFunnel {` to `public class PaymentFunnel {`
 * in your copy. (02_model_solution.java keeps its class package-private only
 * because of the `02_` prefix on the filename.)
 *
 * Fifteen test cases. Each prints PASS or FAIL with a short reason.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// NOTE on the filename: Java would normally require this file to be named
// TestCases.java because the class below is `public`. The class is kept
// package-private so the file can keep its `01_` prefix and still compile.
class TestCases {

    private static final String[] VALID_STATUSES =
            {"INIT", "PROCESSING", "COMPLETED", "FAILED", "REFUNDED"};

    /** A tiny record-like event holder used only by the harness. */
    private static final class Event {
        final double ts;
        final String pid;
        final String status;
        Event(double ts, String pid, String status) {
            this.ts = ts;
            this.pid = pid;
            this.status = status;
        }
    }

    private static Event ev(double ts, String pid, String status) {
        return new Event(ts, pid, status);
    }

    private static Map<String, Integer> zeroCounts() {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (String s : VALID_STATUSES) {
            m.put(s, 0);
        }
        return m;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> run(List<Event> events, double now) {
        PaymentFunnel pf = new PaymentFunnel();
        for (Event e : events) {
            pf.ingest(e.ts, e.pid, e.status);
        }
        return pf.getFunnelMetrics(now);
    }

    @SuppressWarnings("unchecked")
    private static boolean check(String label,
                                 Map<String, Object> got,
                                 Map<String, Integer> wantCountsPartial,
                                 List<String> wantStuck) {
        if (got == null || !got.containsKey("counts") || !got.containsKey("stuck")) {
            System.out.println("FAIL  " + label + " — return shape wrong: " + got);
            return false;
        }
        Object countsObj = got.get("counts");
        Object stuckObj = got.get("stuck");
        if (!(countsObj instanceof Map) || !(stuckObj instanceof List)) {
            System.out.println("FAIL  " + label + " — counts/stuck wrong types: " + got);
            return false;
        }
        Map<String, Integer> gotCounts = (Map<String, Integer>) countsObj;
        List<String> gotStuck = new ArrayList<>((List<String>) stuckObj);
        Collections.sort(gotStuck);
        List<String> wantStuckSorted = new ArrayList<>(wantStuck);
        Collections.sort(wantStuckSorted);

        Map<String, Integer> expectedCounts = zeroCounts();
        expectedCounts.putAll(wantCountsPartial);

        // Compare counts pairwise rather than relying on Map.equals (which would
        // be sensitive to Integer vs int boxing edge cases).
        for (String s : VALID_STATUSES) {
            int wantVal = expectedCounts.get(s);
            int gotVal = gotCounts.getOrDefault(s, 0);
            if (wantVal != gotVal) {
                System.out.println("FAIL  " + label + " — counts mismatch");
                System.out.println("       want: " + expectedCounts);
                System.out.println("       got:  " + gotCounts);
                return false;
            }
        }

        if (!gotStuck.equals(wantStuckSorted)) {
            System.out.println("FAIL  " + label + " — stuck mismatch");
            System.out.println("       want: " + wantStuckSorted);
            System.out.println("       got:  " + gotStuck);
            return false;
        }

        System.out.println("PASS  " + label);
        return true;
    }

    private static Map<String, Integer> counts(Object... kvs) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < kvs.length; i += 2) {
            m.put((String) kvs[i], (Integer) kvs[i + 1]);
        }
        return m;
    }

    public static void main(String[] args) {
        System.out.println("Loaded PaymentFunnel — running 15 tests.\n");
        List<Boolean> results = new ArrayList<>();

        // T01 — single payment INIT -> PROCESSING -> COMPLETED, well under 30s.
        results.add(check(
                "T01 single payment INIT->PROCESSING->COMPLETED",
                run(Arrays.asList(
                        ev(100.0, "p1", "INIT"),
                        ev(101.0, "p1", "PROCESSING"),
                        ev(102.0, "p1", "COMPLETED")), 110.0),
                counts("COMPLETED", 1),
                List.of()));

        // T02 — payment stuck in PROCESSING for 35 seconds — must appear in stuck list.
        results.add(check(
                "T02 payment stuck PROCESSING > 30s",
                run(Arrays.asList(
                        ev(1000.0, "p1", "INIT"),
                        ev(1001.0, "p1", "PROCESSING")), 1036.0),
                counts("PROCESSING", 1),
                List.of("p1")));

        // T03 — boundary at exactly 30s — strict inequality, NOT stuck.
        results.add(check(
                "T03 boundary exactly 30s — not stuck (strict >)",
                run(Arrays.asList(
                        ev(1000.0, "p1", "PROCESSING")), 1030.0),
                counts("PROCESSING", 1),
                List.of()));

        // T04 — boundary at 30.00001s — IS stuck.
        results.add(check(
                "T04 boundary 30.00001s — IS stuck",
                run(Arrays.asList(
                        ev(1000.0, "p1", "PROCESSING")), 1030.00001),
                counts("PROCESSING", 1),
                List.of("p1")));

        // T05 — refund flow: COMPLETED -> REFUNDED, counts swap, no double-count.
        results.add(check(
                "T05 refund: COMPLETED->REFUNDED swaps cleanly",
                run(Arrays.asList(
                        ev(100.0, "p1", "INIT"),
                        ev(101.0, "p1", "PROCESSING"),
                        ev(102.0, "p1", "COMPLETED"),
                        ev(500.0, "p1", "REFUNDED")), 600.0),
                counts("REFUNDED", 1),
                List.of()));

        // T06 — payment that was once stuck, then completed, must NOT appear in stuck list.
        results.add(check(
                "T06 once-stuck then completed — clears from stuck",
                run(Arrays.asList(
                        ev(1000.0, "p1", "PROCESSING"),
                        ev(1040.0, "p1", "COMPLETED")), 1100.0),
                counts("COMPLETED", 1),
                List.of()));

        // T07 — duplicate INIT events (idempotency) — count must remain 1.
        results.add(check(
                "T07 duplicate INIT is idempotent",
                run(Arrays.asList(
                        ev(100.0, "p1", "INIT"),
                        ev(101.0, "p1", "INIT"),
                        ev(102.0, "p1", "INIT")), 110.0),
                counts("INIT", 1),
                List.of()));

        // T08 — duplicate PROCESSING events, must still count once and track the
        // *earliest* PROCESSING timestamp for stuck detection. NOT latest-ts-wins.
        results.add(check(
                "T08 duplicate PROCESSING — counted once, earliest ts wins",
                run(Arrays.asList(
                        ev(1000.0, "p1", "PROCESSING"),
                        ev(1005.0, "p1", "PROCESSING"),
                        ev(1010.0, "p1", "PROCESSING")), 1031.0),
                counts("PROCESSING", 1),
                List.of("p1")));   // earliest ts 1000.0, elapsed 31s > 30s

        // T09 — unknown status string is silently dropped, no crash.
        results.add(check(
                "T09 unknown status string — silently dropped",
                run(Arrays.asList(
                        ev(100.0, "p1", "INIT"),
                        ev(101.0, "p1", "WIBBLE"),
                        ev(102.0, "p1", "PROCESSING")), 110.0),
                counts("PROCESSING", 1),
                List.of()));

        // T10 — out-of-order stale event for the same payment is dropped.
        results.add(check(
                "T10 out-of-order stale event dropped",
                run(Arrays.asList(
                        ev(200.0, "p1", "PROCESSING"),
                        ev(210.0, "p1", "COMPLETED"),
                        ev(150.0, "p1", "FAILED")),   // stale, older ts than last seen
                        300.0),
                counts("COMPLETED", 1),
                List.of()));

        // T11 — three payments in three different terminal statuses.
        results.add(check(
                "T11 three payments in three terminal statuses",
                run(Arrays.asList(
                        ev(100.0, "a", "COMPLETED"),
                        ev(101.0, "b", "FAILED"),
                        ev(102.0, "c", "REFUNDED")), 200.0),
                counts("COMPLETED", 1, "FAILED", 1, "REFUNDED", 1),
                List.of()));

        // T12 — mixed funnel: two stuck, two completed, one INIT, one failed.
        List<Event> t12 = Arrays.asList(
                ev(1000.0, "a", "PROCESSING"),   // stuck (60s old)
                ev(1005.0, "b", "PROCESSING"),   // stuck (55s old)
                ev(1010.0, "c", "INIT"),
                ev(1020.0, "d", "PROCESSING"),
                ev(1030.0, "d", "COMPLETED"),
                ev(1040.0, "e", "PROCESSING"),
                ev(1045.0, "e", "FAILED"),
                ev(1050.0, "f", "PROCESSING"),
                ev(1055.0, "f", "COMPLETED")
        );
        results.add(check(
                "T12 mixed funnel with two stuck payments",
                run(t12, 1060.0),
                counts("INIT", 1, "PROCESSING", 2, "COMPLETED", 2, "FAILED", 1),
                List.of("a", "b")));

        // T13 — refund of a payment that was never in our funnel? Treat as new payment
        // whose latest status is REFUNDED. (Spec: paymentId is the unique key; the
        // funnel reflects current state, not history.)
        results.add(check(
                "T13 standalone REFUNDED event — counted once",
                run(Arrays.asList(
                        ev(500.0, "ghost", "REFUNDED")), 600.0),
                counts("REFUNDED", 1),
                List.of()));

        // T14 — 1000-event stress test. Build deterministic events, expect a specific
        // split. Half the payments stay in PROCESSING (and most should be stuck given
        // we query at the end).
        Object[] stress = buildStress();
        @SuppressWarnings("unchecked")
        List<Event> stressEvents = (List<Event>) stress[0];
        double stressNow = (Double) stress[1];
        @SuppressWarnings("unchecked")
        Map<String, Integer> stressCounts = (Map<String, Integer>) stress[2];
        @SuppressWarnings("unchecked")
        List<String> stressStuck = (List<String>) stress[3];
        results.add(check(
                "T14 1000-event stress test",
                run(stressEvents, stressNow),
                stressCounts,
                stressStuck));

        // T15 — same paymentId cycles through every status; final state must be REFUNDED
        // and stuck list must be empty.
        results.add(check(
                "T15 single payment touches all 5 statuses — ends REFUNDED",
                run(Arrays.asList(
                        ev(100.0, "p1", "INIT"),
                        ev(101.0, "p1", "PROCESSING"),
                        ev(102.0, "p1", "COMPLETED"),
                        ev(103.0, "p1", "FAILED"),       // weird but allowed
                        ev(104.0, "p1", "REFUNDED")), 200.0),
                counts("REFUNDED", 1),
                List.of()));

        // Report
        int passed = 0;
        for (boolean r : results) {
            if (r) passed++;
        }
        int total = results.size();
        System.out.println();
        System.out.println("Score: " + passed + "/" + total);
        if (passed == total) {
            System.out.println("All tests passed. Now read 03_walkthrough.md and self-score with 04_scoring_rubric.md.");
            System.exit(0);
        } else {
            System.out.println("Some tests failed. Re-read the failures, fix, re-run.");
            System.exit(1);
        }
    }

    private static Object[] buildStress() {
        List<Event> events = new ArrayList<>();
        Map<String, Integer> expectedCounts = zeroCounts();
        List<String> expectedStuck = new ArrayList<>();
        double baseTs = 10_000.0;
        for (int i = 0; i < 1000; i++) {
            String pid = String.format("pay_%04d", i);
            // everyone starts at INIT then PROCESSING
            events.add(ev(baseTs + i * 0.001, pid, "INIT"));
            events.add(ev(baseTs + i * 0.001 + 0.0005, pid, "PROCESSING"));
            // 500 of them transition to a terminal state, others stay in PROCESSING.
            // The Python harness picked terminal from ["COMPLETED", "FAILED"][i % 4 // 2].
            // For even i, i % 4 is either 0 or 2. (i % 4) // 2 is 0 when i%4==0 and 1 when i%4==2.
            // Java equivalent for even i: ((i % 4) / 2) gives 0 or 1, selecting COMPLETED or FAILED.
            if (i % 2 == 0) {
                int terminalIdx = (i % 4) / 2;
                String terminal = (terminalIdx == 0) ? "COMPLETED" : "FAILED";
                events.add(ev(baseTs + i * 0.001 + 0.0008, pid, terminal));
                expectedCounts.merge(terminal, 1, Integer::sum);
            } else {
                expectedCounts.merge("PROCESSING", 1, Integer::sum);
                expectedStuck.add(pid);   // PROCESSING ts ~10000, now=20000 — very stuck
            }
        }
        double now = 20_000.0;
        return new Object[]{events, now, expectedCounts, expectedStuck};
    }
}

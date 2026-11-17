/*
 * Mock Simulator Round 4 — Cred Backend SDE-1 Tech Round
 * Auto-grader for YourSolution.java
 *
 * Usage:
 *   1. Write your RewardEngine class in `YourSolution.java` (same folder).
 *      The class must expose:
 *        - addRule(String, String, String, int, double)
 *        - record Purchase(String category, int amount)
 *        - record CashbackResult(double amount, List<String> path)
 *        - computeBestCashback(List<Purchase>)
 *        - computeBestCashbackAdditive(List<Purchase>, double)
 *
 *   2. From this folder, compile and run:
 *        javac YourSolution.java 01_test_cases.java
 *        java TestCases
 *
 *   3. The script reports pass/fail for each of 20 hidden cases
 *      (15 main + 5 follow-up) plus a final score.
 *
 * If YourSolution.java is missing, compile against the model as a fallback
 * (`javac 02_model_solution.java 01_test_cases.java`) — but your real score
 * is 0/20 until you write your own.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestCases {

    // ---------- Comparison helpers ------------------------------------------

    /** Floats — compare with a tolerance because percentages divide by 100. */
    private static boolean amountsEqual(double got, double expected) {
        return Math.abs(got - expected) < 1e-6;
    }

    private static boolean resultMatches(RewardEngine.CashbackResult got,
                                         double expectedAmount,
                                         List<List<String>> expectedPaths) {
        if (got == null) return false;
        if (!amountsEqual(got.amount(), expectedAmount)) return false;
        if (got.path() == null) return false;
        // Some test cases have multiple acceptable paths (tie on cashback).
        for (List<String> ok : expectedPaths) {
            if (got.path().equals(ok)) return true;
        }
        return false;
    }

    // ---------- Rule fixtures -----------------------------------------------
    // Each fixture is a list of RuleSpec records — applied in order to a
    // fresh RewardEngine.

    record RuleSpec(String ruleId, String parentId, String category,
                    int threshold, double percent) {}

    private static final List<RuleSpec> FIXTURE_EMPTY = List.of();

    private static final List<RuleSpec> FIXTURE_SINGLE_ROOT = List.of(
            new RuleSpec("R1", null, "travel", 10000, 5.0)
    );

    private static final List<RuleSpec> FIXTURE_ROOT_AND_TWO_CHILDREN = List.of(
            new RuleSpec("R1", null, "food", 500, 3.0),
            new RuleSpec("R2", "R1", "fuel", 1000, 6.0),
            new RuleSpec("R3", "R1", "fuel", 3000, 10.0)
    );

    private static final List<RuleSpec> FIXTURE_DEEP_FOUR_LEVELS = List.of(
            new RuleSpec("R1", null, "A", 100, 2.0),
            new RuleSpec("R2", "R1", "B", 200, 4.0),
            new RuleSpec("R3", "R2", "C", 300, 6.0),
            new RuleSpec("R4", "R3", "D", 400, 20.0)
    );

    private static final List<RuleSpec> FIXTURE_BRANCHING = List.of(
            new RuleSpec("R1", null, "food", 1000, 5.0),
            new RuleSpec("R2", "R1", "fuel", 500, 8.0),
            new RuleSpec("R3", "R1", "food", 5000, 12.0),
            new RuleSpec("R4", "R2", "fuel", 2000, 15.0)
    );

    private static final List<RuleSpec> FIXTURE_TWO_ROOTS = List.of(
            new RuleSpec("R1", null, "food", 100, 5.0),
            new RuleSpec("R2", null, "fuel", 100, 7.0),
            new RuleSpec("R3", "R1", "food", 1000, 10.0),
            new RuleSpec("R4", "R2", "fuel", 1000, 12.0)
    );

    private static final List<RuleSpec> FIXTURE_ZERO_PERCENT_ROOT = List.of(
            new RuleSpec("R1", null, "food", 100, 0.0),
            new RuleSpec("R2", "R1", "food", 500, 10.0)
    );

    // ---------- Helpers ------------------------------------------------------

    private static RewardEngine buildEngine(List<RuleSpec> fixture) {
        RewardEngine engine = new RewardEngine();
        for (RuleSpec r : fixture) {
            engine.addRule(r.ruleId(), r.parentId(), r.category(),
                    r.threshold(), r.percent());
        }
        return engine;
    }

    private static List<RewardEngine.Purchase> purchases(Object... pairs) {
        List<RewardEngine.Purchase> out = new ArrayList<>();
        for (int i = 0; i < pairs.length; i += 2) {
            out.add(new RewardEngine.Purchase(
                    (String) pairs[i], (Integer) pairs[i + 1]));
        }
        return out;
    }

    // ---------- Main-problem cases (15) -------------------------------------

    record MainCase(String label, List<RuleSpec> fixture,
                    List<RewardEngine.Purchase> purchases,
                    double expectedAmount,
                    List<List<String>> expectedPaths) {}

    private static final List<MainCase> MAIN_CASES = List.of(
            // 1 — single rule, no match (cashback 0)
            new MainCase("M1 single root no match",
                    FIXTURE_SINGLE_ROOT,
                    purchases("food", 2000),
                    0.0, List.of(List.of())),

            // 2 — single rule, matches
            new MainCase("M2 single root matches",
                    FIXTURE_SINGLE_ROOT,
                    purchases("travel", 12000),
                    600.0, List.of(List.of("R1"))),

            // 3 — root + two children, one child meets threshold
            new MainCase("M3 two children one match",
                    FIXTURE_ROOT_AND_TWO_CHILDREN,
                    purchases("food", 800, "fuel", 1500),
                    90.0, List.of(List.of("R1", "R2"))),

            // 4 — root + two children, both children meet threshold
            new MainCase("M4 two children both match",
                    FIXTURE_ROOT_AND_TWO_CHILDREN,
                    purchases("food", 800, "fuel", 4000),
                    400.0, List.of(List.of("R1", "R3"))),

            // 5 — deep nesting four levels, all match
            new MainCase("M5 deep four-level chain",
                    FIXTURE_DEEP_FOUR_LEVELS,
                    purchases("A", 150, "B", 250, "C", 350, "D", 500),
                    100.0, List.of(List.of("R1", "R2", "R3", "R4"))),

            // 6 — deep nesting, breaks at level 3 (R3 fails)
            new MainCase("M6 chain breaks at level 3",
                    FIXTURE_DEEP_FOUR_LEVELS,
                    purchases("A", 150, "B", 250, "C", 50, "D", 500),
                    10.0, List.of(List.of("R1", "R2"))),

            // 7 — branching: best is at root R3 vs deeper R4 -- R3 wins
            new MainCase("M7 branching wider rule wins",
                    FIXTURE_BRANCHING,
                    purchases("food", 6000, "fuel", 2500),
                    720.0, List.of(List.of("R1", "R3"))),

            // 8 — branching: deeper R4 wins over R3
            new MainCase("M8 branching deeper rule wins",
                    FIXTURE_BRANCHING,
                    purchases("food", 1500, "fuel", 3000),
                    450.0, List.of(List.of("R1", "R2", "R4"))),

            // 9 — branching: only root matches (no child threshold met)
            new MainCase("M9 only root matches",
                    FIXTURE_BRANCHING,
                    purchases("food", 1200, "fuel", 100),
                    60.0, List.of(List.of("R1"))),

            // 10 — two roots: pick the higher-yielding tree
            new MainCase("M10 two roots pick best",
                    FIXTURE_TWO_ROOTS,
                    purchases("food", 200, "fuel", 2000),
                    240.0, List.of(List.of("R2", "R4"))),

            // 11 — two roots: the other tree wins
            new MainCase("M11 two roots other wins",
                    FIXTURE_TWO_ROOTS,
                    purchases("food", 2000, "fuel", 200),
                    200.0, List.of(List.of("R1", "R3"))),

            // 12 — empty purchases (no rule can match)
            new MainCase("M12 empty purchases",
                    FIXTURE_BRANCHING,
                    purchases(),
                    0.0, List.of(List.of())),

            // 13 — purchases match no category in any rule
            new MainCase("M13 purchases mismatch category",
                    FIXTURE_BRANCHING,
                    purchases("electronics", 99999),
                    0.0, List.of(List.of())),

            // 14 — 0% root + meaningful child (still 0 at root, child wins)
            new MainCase("M14 zero-percent root child wins",
                    FIXTURE_ZERO_PERCENT_ROOT,
                    purchases("food", 800),
                    80.0, List.of(List.of("R1", "R2"))),

            // 15 — same category twice in purchases (must be summed)
            new MainCase("M15 same category summed",
                    FIXTURE_SINGLE_ROOT,
                    purchases("travel", 6000, "travel", 5000),
                    550.0, List.of(List.of("R1")))
    );

    // ---------- Follow-up cases (5) -----------------------------------------

    record FollowupCase(String label, List<RuleSpec> fixture,
                        List<RewardEngine.Purchase> purchases,
                        double dailyCap, double expectedAmount,
                        List<List<String>> expectedPaths) {}

    private static final List<FollowupCase> FOLLOWUP_CASES = List.of(
            // F1 — additive: full chain sums under the cap, full path wins
            // 3 + 10 + 21 + 100 = 134
            new FollowupCase("F1 chain sums under cap",
                    FIXTURE_DEEP_FOUR_LEVELS,
                    purchases("A", 150, "B", 250, "C", 350, "D", 500),
                    1000.0,
                    134.0, List.of(List.of("R1", "R2", "R3", "R4"))),

            // F2 — additive: cap kicks in mid-chain
            // 3+10+21 = 34 (< cap 50), +100 = 134 capped at 50.
            // The cross-over happens at R4 where capped == 50.
            new FollowupCase("F2 cap kicks in",
                    FIXTURE_DEEP_FOUR_LEVELS,
                    purchases("A", 150, "B", 250, "C", 350, "D", 500),
                    50.0,
                    50.0, List.of(
                            List.of("R1", "R2", "R3"),
                            List.of("R1", "R2", "R3", "R4"))),

            // F3 — additive on branching tree, cap is high enough that nothing
            // is clipped. R1=300, R3=720 along [R1,R3] sums to 1020.
            // R1=300, R2=200, R4=375 along [R1,R2,R4] sums to 875.
            // Best is [R1, R3] at 1020.
            new FollowupCase("F3 branching additive wins",
                    FIXTURE_BRANCHING,
                    purchases("food", 6000, "fuel", 2500),
                    10000.0,
                    1020.0, List.of(List.of("R1", "R3"))),

            // F4 — additive with no matches at all
            new FollowupCase("F4 no matches additive",
                    FIXTURE_BRANCHING,
                    purchases(),
                    100.0,
                    0.0, List.of(List.of())),

            // F5 — additive: cap exactly equals total
            // R1 = 800*3/100 = 24, R3 = 4000*10/100 = 400, sum = 424.
            new FollowupCase("F5 cap exactly equals total",
                    FIXTURE_ROOT_AND_TWO_CHILDREN,
                    purchases("food", 800, "fuel", 4000),
                    424.0,
                    424.0, List.of(List.of("R1", "R3")))
    );

    // ---------- Runner -------------------------------------------------------

    private static int runMainBlock() {
        System.out.println("\n--- Main problem — computeBestCashback (15 cases) ---");
        int passed = 0;
        for (MainCase c : MAIN_CASES) {
            try {
                RewardEngine engine = buildEngine(c.fixture());
                RewardEngine.CashbackResult got = engine.computeBestCashback(c.purchases());
                if (resultMatches(got, c.expectedAmount(), c.expectedPaths())) {
                    System.out.println("  [PASS] " + c.label());
                    passed++;
                } else {
                    System.out.println("  [FAIL] " + c.label());
                }
            } catch (Exception e) {
                System.out.println("  [FAIL] " + c.label()
                        + "  (threw " + e.getClass().getSimpleName()
                        + ": " + e.getMessage() + ")");
            }
        }
        return passed;
    }

    private static int runFollowupBlock() {
        System.out.println("\n--- Follow-up — computeBestCashbackAdditive (5 cases) ---");
        int passed = 0;
        for (FollowupCase c : FOLLOWUP_CASES) {
            try {
                RewardEngine engine = buildEngine(c.fixture());
                RewardEngine.CashbackResult got = engine.computeBestCashbackAdditive(
                        c.purchases(), c.dailyCap());
                if (resultMatches(got, c.expectedAmount(), c.expectedPaths())) {
                    System.out.println("  [PASS] " + c.label());
                    passed++;
                } else {
                    System.out.println("  [FAIL] " + c.label());
                }
            } catch (Exception e) {
                System.out.println("  [FAIL] " + c.label()
                        + "  (threw " + e.getClass().getSimpleName()
                        + ": " + e.getMessage() + ")");
            }
        }
        return passed;
    }

    public static void main(String[] args) {
        System.out.println("Running mock-simulator grader for RewardEngine");

        int mainScore = runMainBlock();
        int followupScore = runFollowupBlock();
        int total = mainScore + followupScore;

        System.out.println("\n========================================");
        System.out.println("  Main score:       " + mainScore + "/15");
        System.out.println("  Follow-up score:  " + followupScore + "/5");
        System.out.println("  TOTAL:            " + total + "/20");
        System.out.println("========================================");

        if (total == 20) {
            System.out.println(
                    "\nClean sweep. Now grade yourself on style + clarification habit using 04_scoring_rubric.md.");
        } else if (total >= 16) {
            System.out.println(
                    "\nStrong pass for Cred. Open 03_walkthrough.md and diff your approach against the model.");
        } else if (total >= 12) {
            System.out.println(
                    "\nMixed pass. Likely a path-tracking bug or follow-up gap. Walkthrough will show you where.");
        } else {
            System.out.println(
                    "\nBelow the bar for Cred Backend SDE-1. Re-read the trees + backtracking patterns and try again in 72 hours.");
        }
    }
}

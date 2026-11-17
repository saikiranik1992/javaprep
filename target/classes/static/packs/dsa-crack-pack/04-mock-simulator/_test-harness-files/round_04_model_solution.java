/*
 * Mock Simulator Round 4 — Cred Backend SDE-1 Tech Round
 * Model solution for the RewardEngine problem + the additive-cap follow-up.
 *
 * This is the form a strong Cred candidate would type live. It uses:
 * - Java `record` for the Rule, Purchase, and CashbackResult types
 *   (production-grade craft Cred expects — same energy as Python's @dataclass)
 * - Full generic types on every collection (no raw types)
 * - Explicit recursive DFS with the base case at the top
 * - A small mutable "best so far" holder (BestState) instead of nonlocal —
 *   Java does not have nonlocal, and lambda-captured fields must be effectively
 *   final, so a one-field holder is the idiomatic substitute.
 * - A deliberate note on why memoisation does not help here.
 *
 * Compile and grade with:
 *     javac 02_model_solution.java 01_test_cases.java
 *     java TestCases
 *
 * The grader looks for `YourSolution.java` first and falls back to this file
 * only as a sanity check (real score 0/20 in that case).
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class RewardEngine {

    // ---------- Data model --------------------------------------------------

    /** One row in the rewards rule table. */
    record Rule(String ruleId, String parentId, String category,
                int threshold, double percent) {}

    /** Single line item from the user's purchase history. */
    public record Purchase(String category, int amount) {}

    /** The (amount, path) pair returned by both compute methods. */
    public record CashbackResult(double amount, List<String> path) {}

    // ---------- Internal layout ---------------------------------------------
    //   rules      : ruleId -> Rule
    //   childrenOf : ruleId -> list of child ruleIds
    //   roots      : list of ruleIds whose parentId is null
    //
    // We do NOT enforce add-order: a child rule may be registered before its
    // parent. We index both directions on every addRule call and resolve the
    // walk lazily at compute time, starting only from roots.

    private final Map<String, Rule> rules = new HashMap<>();
    private final Map<String, List<String>> childrenOf = new HashMap<>();
    private final List<String> roots = new ArrayList<>();

    // ---------- Registration ------------------------------------------------

    public void addRule(String ruleId, String parentId, String category,
                        int threshold, double percent) {
        Rule rule = new Rule(ruleId, parentId, category, threshold, percent);
        rules.put(ruleId, rule);
        if (parentId == null) {
            if (!roots.contains(ruleId)) {
                roots.add(ruleId);
            }
        } else {
            childrenOf.computeIfAbsent(parentId, k -> new ArrayList<>()).add(ruleId);
        }
    }

    // ---------- Helpers -----------------------------------------------------

    /** Sum purchase amounts by category in one pass. */
    private Map<String, Long> categoryTotals(List<Purchase> purchases) {
        Map<String, Long> totals = new HashMap<>();
        for (Purchase p : purchases) {
            totals.merge(p.category(), (long) p.amount(), Long::sum);
        }
        return totals;
    }

    private boolean ruleMatches(Rule rule, Map<String, Long> totals) {
        return totals.getOrDefault(rule.category(), 0L) > rule.threshold();
    }

    private double ruleCashback(Rule rule, Map<String, Long> totals) {
        long spent = totals.getOrDefault(rule.category(), 0L);
        return (spent * rule.percent()) / 100.0;
    }

    // ---------- Mutable "best so far" holder --------------------------------
    // Java lambdas can only capture effectively final references, and we have
    // no `nonlocal` keyword. A one-instance holder gives us the same shape
    // as the Python nonlocal pattern without falling back to int[]/double[]
    // single-cell arrays (which are the lazy idiom and reviewer-flagged).

    private static final class BestState {
        double amount = 0.0;
        List<String> path = List.of();
    }

    // ---------- Main problem ------------------------------------------------

    /**
     * Best single-rule cashback achievable along any valid root-to-node walk.
     *
     * Complexity:
     *   Tree has N rules. We touch each rule at most once because the tree is
     *   a proper tree (no shared parents). So O(N) rule visits plus O(P) for
     *   the category-totals pre-pass = O(N + P).
     *
     *   Memoisation across calls does NOT help — the result depends on the
     *   `purchases` argument, not just on the rule subtree. Two different
     *   computeBestCashback calls on the same engine can pass different
     *   purchases, so caching `bestCashbackAt(ruleId)` would return stale
     *   values. Within a single call, each rule is visited once anyway
     *   because there are no shared parents.
     */
    public CashbackResult computeBestCashback(List<Purchase> purchases) {
        Map<String, Long> totals = categoryTotals(purchases);
        BestState best = new BestState();

        for (String rootId : roots) {
            dfsBest(rootId, new ArrayList<>(), totals, best);
        }
        return new CashbackResult(best.amount, best.path);
    }

    private void dfsBest(String ruleId, List<String> pathSoFar,
                         Map<String, Long> totals, BestState best) {
        Rule rule = rules.get(ruleId);
        if (!ruleMatches(rule, totals)) {
            // Parent did not match -> children unreachable on this walk.
            return;
        }
        List<String> extendedPath = new ArrayList<>(pathSoFar);
        extendedPath.add(ruleId);

        double thisCashback = ruleCashback(rule, totals);
        if (thisCashback > best.amount) {
            best.amount = thisCashback;
            best.path = extendedPath;
        }
        for (String childId : childrenOf.getOrDefault(ruleId, List.of())) {
            dfsBest(childId, extendedPath, totals, best);
        }
    }

    // ---------- Follow-up: additive stacking with a daily cap ---------------

    /**
     * Same tree, same matching condition, but cashback values along a path
     * ADD UP. The total on a path is min(sum, dailyCap). Return the path
     * that maximises this capped total.
     *
     * Cap arithmetic:
     *   We cap once at the end of each candidate path, not greedily as we
     *   descend. Capping greedily would incorrectly truncate a path whose
     *   later rules still contribute extra-percent if the cap had not yet
     *   been hit. Cap arithmetic = `Math.min(runningSum, dailyCap)`.
     *
     *   Once `capped >= dailyCap` we short-circuit further descent — no
     *   deeper rule can improve the capped total.
     */
    public CashbackResult computeBestCashbackAdditive(List<Purchase> purchases,
                                                     double dailyCap) {
        Map<String, Long> totals = categoryTotals(purchases);
        BestState best = new BestState();

        for (String rootId : roots) {
            dfsAdditive(rootId, new ArrayList<>(), 0.0, totals, dailyCap, best);
        }
        return new CashbackResult(best.amount, best.path);
    }

    private void dfsAdditive(String ruleId, List<String> pathSoFar,
                             double sumSoFar, Map<String, Long> totals,
                             double dailyCap, BestState best) {
        Rule rule = rules.get(ruleId);
        if (!ruleMatches(rule, totals)) {
            return;
        }
        List<String> extendedPath = new ArrayList<>(pathSoFar);
        extendedPath.add(ruleId);

        double runningSum = sumSoFar + ruleCashback(rule, totals);
        double capped = Math.min(runningSum, dailyCap);
        if (capped > best.amount) {
            best.amount = capped;
            best.path = extendedPath;
        }
        // Once we've hit the cap, no descendant can improve the capped total
        // — only equal it. Stop descending.
        if (capped >= dailyCap) {
            return;
        }
        for (String childId : childrenOf.getOrDefault(ruleId, List.of())) {
            dfsAdditive(childId, extendedPath, runningSum, totals, dailyCap, best);
        }
    }
}

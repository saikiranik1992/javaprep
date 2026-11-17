/*
 * Mock Simulator Round 5 — Model Solution (Java 17+)
 * RateLimiter with dual sliding-window constraints:
 *   - max 10 requests per user per 1-second  window
 *   - max 100 requests per user per 10-second window
 *
 * Design choices documented inline. See 03_walkthrough.md for the
 * narrative behind every decision.
 *
 * NOTE: this file is named 02_model_solution.java so it cannot declare
 * a `public class` (Java requires the public class name to match the
 * filename, and identifiers cannot start with a digit). The class is
 * package-private; the test harness sees it because both files are
 * compiled together in the same default package.
 */

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class RateLimiter {

    // Window sizes and per-window caps live as named constants. The panel
    // will quiz on "what if the SLA changes to 20/sec?" — having them
    // parameterised in one place is the right answer.
    private static final double WINDOW_1S_SECONDS = 1.0;
    private static final double WINDOW_10S_SECONDS = 10.0;
    private static final int CAP_1S = 10;
    private static final int CAP_10S = 100;

    /*
     * UserBucket holds the two deques tracking allowed-request timestamps
     * for one user.
     *
     * Invariant: both deques are sorted in non-decreasing order of
     * timestamp, because isAllowed only ever appends with the current
     * nowSeconds and nowSeconds is non-decreasing per user.
     *
     * Memory: bounded by (CAP_1S + CAP_10S) doubles per active user
     * ~ 110 doubles ~ 880 bytes payload + ArrayDeque overhead.
     * The 1-second deque is a strict subset of the 10-second deque
     * timestamp-wise, but we keep them separate so each pop step is
     * O(1) without re-scanning.
     *
     * NOTE: static inner class (not record) because the deques are
     * mutated in place on every allowed request. A record gives us
     * immutable fields, which forces an alloc-and-replace pattern
     * that defeats the O(1) memory bound.
     */
    static class UserBucket {
        Deque<Double> window1s = new ArrayDeque<>();
        Deque<Double> window10s = new ArrayDeque<>();
    }

    // Plain HashMap, not a computeIfAbsent default-supplier idiom inline,
    // so we can later add idle-user eviction without surprises.
    private final Map<String, UserBucket> buckets = new HashMap<>();

    /**
     * Returns true if a request from `userId` at `nowSeconds` is allowed
     * under both the 1-second and 10-second caps; false otherwise.
     *
     * Allowed requests are recorded; rejected requests are not.
     * Amortised O(1) time, O(CAP_1S + CAP_10S) space per active user.
     *
     * Not thread-safe. See walkthrough minute 60 for the lock discussion.
     */
    public boolean isAllowed(String userId, double nowSeconds) {
        UserBucket bucket = buckets.computeIfAbsent(userId, k -> new UserBucket());

        // Lazy eviction — drop timestamps that have fallen out of each
        // window. Amortised O(1) per call because every timestamp is
        // popped at most once across the limiter's lifetime.
        double cutoff1s = nowSeconds - WINDOW_1S_SECONDS;
        double cutoff10s = nowSeconds - WINDOW_10S_SECONDS;
        evictOlderThan(bucket.window1s, cutoff1s);
        evictOlderThan(bucket.window10s, cutoff10s);

        // Joint enforcement — both must pass. Fail-fast on the 1-sec cap
        // because it's the tighter constraint in the common case.
        if (bucket.window1s.size() >= CAP_1S) {
            return false;
        }
        if (bucket.window10s.size() >= CAP_10S) {
            return false;
        }

        // Allowed — record and return.
        bucket.window1s.offerLast(nowSeconds);
        bucket.window10s.offerLast(nowSeconds);
        return true;
    }

    private void evictOlderThan(Deque<Double> dq, double cutoff) {
        // The window is half-open on the older side: a timestamp
        // exactly equal to `cutoff` is *outside* the window.
        // i.e. window is (cutoff, nowSeconds] — see walkthrough
        // minute 55 for why this matters at exact boundary times.
        while (!dq.isEmpty() && dq.peekFirst() <= cutoff) {
            dq.pollFirst();
        }
    }

    // ----- Optional: housekeeping for very-long-lived limiters -----

    /**
     * Drops users whose 10-second window is empty at `nowSeconds`.
     * Returns the number of users dropped.
     *
     * Not called on the hot path — meant for a slow background sweep
     * (e.g. once per minute) to keep `buckets` from accumulating
     * millions of dormant user IDs over time.
     */
    public int evictIdleUsers(double nowSeconds) {
        double cutoff10s = nowSeconds - WINDOW_10S_SECONDS;
        List<String> dead = new ArrayList<>();
        for (Map.Entry<String, UserBucket> entry : buckets.entrySet()) {
            Deque<Double> dq = entry.getValue().window10s;
            // Cheap check first — peek the newest 10s timestamp.
            if (dq.isEmpty() || dq.peekLast() <= cutoff10s) {
                dead.add(entry.getKey());
            }
        }
        for (String userId : dead) {
            buckets.remove(userId);
        }
        return dead.size();
    }
}

/*
 * ----------------------------------------------------------------------
 * Why this design, in one breath each (the panel will ask):
 *
 * 1. ArrayDeque vs ArrayList: ArrayDeque gives O(1) pollFirst for
 *    eviction and O(1) offerLast for new requests. An ArrayList with
 *    remove(0) would shift the entire backing array — O(n).
 *
 * 2. Two deques vs one deque: with one deque you can derive the 1s
 *    count by scanning back from the tail, but that scan is O(CAP_1S)
 *    in the worst case per call. Two deques gives strict O(1) size
 *    check at the cost of 2x the per-user memory (still bounded by
 *    CAP_1S + CAP_10S = 110 doubles).
 *
 * 3. Lazy eviction vs background sweep: lazy keeps the limiter
 *    self-contained and avoids a background thread coordinating with
 *    the read path. Idle users do accumulate timestamps in their
 *    deques until the next call from that user — but the deque size
 *    is hard-capped, so memory per user is bounded. The
 *    evictIdleUsers helper handles the orthogonal concern of
 *    dropping the map entries themselves.
 *
 * 4. Token bucket alternative: a token bucket gives smoother behaviour
 *    (refill rate of 10/sec with burst capacity 10) and uses O(1)
 *    state per user — just (lastRefillTime, tokensLeft). But layering
 *    two token buckets (10/sec AND 100/10sec) needs careful thought
 *    about how they interact when one is starved and the other is
 *    not. Sliding window is more directly auditable: "the user made
 *    these 100 requests in this window, here are their timestamps".
 *    For a rate limiter that has to defend itself in a postmortem,
 *    audit-ability wins. Use token bucket if you need smoother
 *    shaping; use sliding window if you need precise audit.
 *
 * 5. Joint vs independent enforcement: we enforce jointly (both must
 *    pass). If we enforced independently and recorded the timestamp
 *    on the 1-second deque even when the 10-second cap had already
 *    rejected, we would double-charge the user. Joint with a single
 *    append-after-decision keeps the books clean.
 *
 * 6. static class UserBucket vs record: record makes the fields
 *    immutable, which means we'd have to allocate a new UserBucket
 *    on every mutation. The deques themselves are mutable, so the
 *    immutability buys us nothing and costs us an allocation per
 *    call. static class is the right call here. If the bucket were
 *    truly immutable state (e.g. cached reads), record would win.
 *
 * 7. Deque<Double> autoboxing: yes, each entry costs an extra Double
 *    object. A double[] ring buffer would be tighter (8 bytes per
 *    slot, no boxing). For the interview we accept the autoboxing
 *    tax — it's the readable form. In production we'd profile and,
 *    if the limiter is hot, switch to a double[] ring buffer with
 *    head/tail indices. Flag this if asked.
 * ----------------------------------------------------------------------
 */

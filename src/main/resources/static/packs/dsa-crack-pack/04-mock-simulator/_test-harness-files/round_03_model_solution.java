/*
 * Mock Simulator Round 3 — Razorpay SDE-1 model solution.
 *
 * PaymentFunnel — ingest payment events from a stream, return funnel counts +
 * stuck-payment list on demand.
 *
 * Design notes (what you would say out loud in the round):
 *
 * 1. Two hash maps are the workhorse.
 *    - currentStatus: paymentId -> latest status string.
 *    - processingSince: paymentId -> timestamp when it entered PROCESSING.
 *      A payment is present in this map iff its current status is PROCESSING.
 * 2. A counts map tracks the live size of each status bucket. We mutate it on
 *    every status transition (decrement old bucket, increment new bucket). That
 *    keeps the funnel query O(1) for counts. Map.merge with Integer::sum is
 *    the idiomatic Java 17+ way to do "increment or insert" in one line.
 * 3. The stuck list is a linear sweep over processingSince at query time.
 *    |processingSince| <= number of payments currently in PROCESSING, which is
 *    O(P) where P is the active processing set — not O(N) over all events.
 * 4. Idempotency: if the same (paymentId, status) lands twice, the second one
 *    is a no-op. Same-status replays do not double-count.
 * 5. Out-of-order events: if a stale event arrives with a timestamp earlier
 *    than what we have already seen for that payment, we ignore it. The
 *    interviewer may push back on this — discuss it in the follow-up. For the
 *    stated spec it is the safer default.
 *
 * Earliest-PROCESSING-timestamp invariant (NOT latest-ts-wins):
 *   On a duplicate PROCESSING event for a payment already in PROCESSING, we
 *   KEEP the earliest timestamp we have seen. This is production-correct: the
 *   payment has been stuck since the first PROCESSING event, not since the
 *   most recent duplicate. Test T08 exercises this invariant.
 */

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

// NOTE on the filename: Java would normally require this file to be named
// PaymentFunnel.java because the class below is `public`. To make the file work
// with the `02_` prefix, we keep the class package-private (no `public` modifier).
// The class is still callable from the test harness in the same package.
// When you write your own solution, name the file PaymentFunnel.java and feel
// free to declare the class as `public class PaymentFunnel`.
class PaymentFunnel {

    private static final double STUCK_THRESHOLD_SECONDS = 30.0;
    private static final Set<String> VALID_STATUSES =
            Set.of("INIT", "PROCESSING", "COMPLETED", "FAILED", "REFUNDED");

    /** paymentId -> its current (latest) status. */
    private final Map<String, String> currentStatus = new HashMap<>();
    /** paymentId -> timestamp when it entered PROCESSING (earliest wins). */
    private final Map<String, Double> processingSince = new HashMap<>();
    /** paymentId -> timestamp of the last event we accepted for it. */
    private final Map<String, Double> lastEventTs = new HashMap<>();
    /** status -> running count of distinct paymentIds currently in that status. */
    private final Map<String, Integer> counts = new HashMap<>();

    public PaymentFunnel() {
        // No setup required — all state lives in the maps above.
    }

    /**
     * Accept one event. O(1) amortised.
     *
     * Silently ignores:
     *   - events with unknown status strings,
     *   - duplicate same-status events for the same payment,
     *   - stale events whose timestamp is older than the last event seen for
     *     this paymentId.
     *
     * Earliest-PROCESSING-ts wins on a duplicate PROCESSING — NOT latest-ts.
     */
    public void ingest(double timestamp, String paymentId, String status) {
        if (!VALID_STATUSES.contains(status)) {
            return; // unknown status — drop, do not crash the pipeline
        }

        // Edge case: out-of-order events for the same payment. Drop the stale one.
        Double lastSeen = lastEventTs.get(paymentId);
        if (lastSeen != null && timestamp < lastSeen) {
            return;
        }

        String prevStatus = currentStatus.get(paymentId);

        // Edge case: same payment, same status — idempotent no-op.
        // For a duplicate PROCESSING, this preserves the earliest processingSince
        // timestamp because we never overwrite it below.
        if (prevStatus != null && prevStatus.equals(status)) {
            lastEventTs.put(paymentId, timestamp);
            return;
        }

        // Decrement the previous bucket if there was one.
        if (prevStatus != null) {
            counts.merge(prevStatus, -1, Integer::sum);
        }

        // Increment the new bucket. Map.merge handles the "key absent" case
        // cleanly — no defaultdict needed in Java 17+.
        counts.merge(status, 1, Integer::sum);
        currentStatus.put(paymentId, status);
        lastEventTs.put(paymentId, timestamp);

        // Maintain the processingSince index.
        if (status.equals("PROCESSING")) {
            // Only set if we are entering PROCESSING for the first time
            // (or re-entering after a non-PROCESSING status). The duplicate-
            // PROCESSING case is already handled above with the early return,
            // so this branch only fires when prevStatus != PROCESSING.
            processingSince.put(paymentId, timestamp);
        } else if (prevStatus != null && prevStatus.equals("PROCESSING")) {
            // Leaving PROCESSING for something else — clear the index.
            processingSince.remove(paymentId);
        }
    }

    /**
     * Return the funnel snapshot as of `now`. O(P) where P = |PROCESSING|.
     *
     * Returns a map with two keys:
     *   - "counts": Map<String, Integer> with all five statuses (zeros included).
     *   - "stuck": List<String> of paymentIds in PROCESSING for > 30 seconds.
     */
    public Map<String, Object> getFunnelMetrics(double now) {
        // Build the full five-entry counts map — include zeros for clarity.
        // LinkedHashMap to preserve a predictable iteration order when debugging.
        Map<String, Integer> countsOut = new LinkedHashMap<>();
        countsOut.put("INIT", counts.getOrDefault("INIT", 0));
        countsOut.put("PROCESSING", counts.getOrDefault("PROCESSING", 0));
        countsOut.put("COMPLETED", counts.getOrDefault("COMPLETED", 0));
        countsOut.put("FAILED", counts.getOrDefault("FAILED", 0));
        countsOut.put("REFUNDED", counts.getOrDefault("REFUNDED", 0));

        List<String> stuck = new ArrayList<>();
        for (Map.Entry<String, Double> entry : processingSince.entrySet()) {
            // Strict greater-than. Spec says "more than 30 seconds", not "at least".
            if (now - entry.getValue() > STUCK_THRESHOLD_SECONDS) {
                stuck.add(entry.getKey());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("counts", countsOut);
        result.put("stuck", stuck);
        return result;
    }
}

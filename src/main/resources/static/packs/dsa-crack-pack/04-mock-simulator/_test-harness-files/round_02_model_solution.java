/*
 * Round 2 - Flipkart On-Campus SDE-1 . Model Solution
 *
 * Problem: Given a non-empty array of integers, find the contiguous
 * subarray with the largest product. Return that product.
 *
 * Pattern: 1D DP - Maximum Product Subarray.
 * Key idea: track BOTH max and min ending at each index. A negative
 * element flips the sign - the smallest running product can become
 * the largest after one more multiplication.
 *
 * Time:  O(n)  - one pass.
 * Space: O(1)  - three scalars (curMax, curMin, result).
 *
 * Java 17+ - uses only core language features (no records, no
 * pattern matching). Compiles on any modern JDK.
 */

// Note: package-private (no `public` modifier) so the class name can
// differ from the file name. `javac 02_model_solution.java` compiles
// cleanly; the test harness loads `ModelSolution` directly.
class ModelSolution {

    /**
     * Return the maximum product over all non-empty contiguous
     * subarrays of nums.
     *
     * Why track curMin as well as curMax:
     *   - if nums[i] >= 0, curMax grows by multiplying with the
     *     previous curMax; curMin stays the most-negative running
     *     product
     *   - if nums[i] < 0, the previous curMin (a large negative)
     *     becomes a large positive after multiplication, so it
     *     becomes the new curMax
     */
    public int maxProduct(int[] nums) {
        // Note: problem guarantees nums is non-empty.
        int curMax = nums[0];
        int curMin = nums[0];
        int result = nums[0];

        for (int i = 1; i < nums.length; i++) {
            int x = nums[i];

            // IMPORTANT: compute the new curMax and new curMin from the
            // OLD curMax and curMin in the same step. If we wrote
            //     curMax = Math.max(x, Math.max(curMax * x, curMin * x));
            //     curMin = Math.min(x, Math.min(curMax * x, curMin * x));
            // the second line would read the NEW curMax, not the old
            // one, and the curMin update would silently corrupt on
            // negative-heavy inputs.
            int extMax = curMax * x;
            int extMin = curMin * x;
            int newMax = Math.max(x, Math.max(extMax, extMin));
            int newMin = Math.min(x, Math.min(extMax, extMin));

            curMax = newMax;
            curMin = newMin;
            result = Math.max(result, curMax);
        }

        return result;
    }

    /**
     * Follow-up: return the contiguous subarray that achieves the
     * maximum product, not just the product itself.
     *
     * Same O(n) time and O(1) extra space. We carry the start index
     * of the running curMax window and the running curMin window,
     * plus the locked-in bestStart / bestEnd.
     */
    public int[] maxProductSubarray(int[] nums) {
        int curMax = nums[0];
        int curMin = nums[0];
        int result = nums[0];

        int curMaxStart = 0;
        int curMinStart = 0;
        int bestStart = 0;
        int bestEnd = 0;

        for (int i = 1; i < nums.length; i++) {
            int x = nums[i];
            int extMax = curMax * x;
            int extMin = curMin * x;

            // Choose new curMax from {x, extMax, extMin} and remember
            // which start index that branch corresponds to.
            int newMax;
            int newMaxStart;
            if (x >= extMax && x >= extMin) {
                newMax = x;
                newMaxStart = i;
            } else if (extMax >= extMin) {
                newMax = extMax;
                newMaxStart = curMaxStart;
            } else {
                newMax = extMin;
                newMaxStart = curMinStart;
            }

            // Same dance for curMin.
            int newMin;
            int newMinStart;
            if (x <= extMax && x <= extMin) {
                newMin = x;
                newMinStart = i;
            } else if (extMax <= extMin) {
                newMin = extMax;
                newMinStart = curMaxStart;
            } else {
                newMin = extMin;
                newMinStart = curMinStart;
            }

            curMax = newMax;
            curMaxStart = newMaxStart;
            curMin = newMin;
            curMinStart = newMinStart;

            if (curMax > result) {
                result = curMax;
                bestStart = curMaxStart;
                bestEnd = i;
            }
        }

        // Copy the slice nums[bestStart..bestEnd] into a fresh int[].
        int length = bestEnd - bestStart + 1;
        int[] out = new int[length];
        for (int i = 0; i < length; i++) {
            out[i] = nums[bestStart + i];
        }
        return out;
    }
}

package engine;

/*
 * SuffixArray.java
 *
 * WHAT IS A SUFFIX?
 * A suffix of a string is everything from some starting position to the end.
 * For the text "banana" (length 6), the suffixes are:
 *   index 0: banana
 *   index 1: anana
 *   index 2: nana
 *   index 3: ana
 *   index 4: na
 *   index 5: a
 *
 * WHAT IS A SUFFIX ARRAY?
 * If we sort all these suffixes alphabetically, and then store only the
 * STARTING INDEX of each suffix (in sorted order), we get the Suffix Array.
 *
 * For "banana", sorted suffixes are:
 *   a        (index 5)
 *   ana      (index 3)
 *   anana    (index 1)
 *   banana   (index 0)
 *   na       (index 4)
 *   nana     (index 2)
 *
 * So Suffix Array = [5, 3, 1, 0, 4, 2]
 *
 * WHY IS THIS USEFUL?
 * Once sorted, suffixes that start with the same substring end up next to
 * each other. This lets us quickly find common substrings between two
 * documents by looking at neighboring suffixes in the sorted array
 * (this is exactly what the LCP array + Kasai algorithm will do next).
 *
 * HOW WE BUILD IT: PREFIX DOUBLING
 * Instead of comparing whole suffixes character by character (which is slow),
 * we sort suffixes by their first 1 character, then by their first 2
 * characters, then first 4, then first 8... doubling the compared length
 * each round. We use a "rank" array to remember the relative order from the
 * previous round, so each round's comparison is O(1) instead of O(length).
 *
 * TIME COMPLEXITY: O(n * log(n) * log(n)) approximately, because we do
 * O(log n) rounds, and each round sorts n suffixes using simple comparisons.
 * (We avoid SA-IS because it is too advanced for a DSA student project.)
 *
 * SPACE COMPLEXITY: O(n) extra arrays for rank, temporary rank, and suffix array.
 */
public class SuffixArray {

    // Builds and returns the suffix array (array of starting indices) for "text".
    public static int[] build(String text) {
        int n = text.length();

        // suffixArray[i] will eventually hold the starting index of the
        // i-th smallest suffix.
        int[] suffixArray = new int[n];

        // rank[i] = the current "rank" (relative order) of the suffix
        // starting at index i, based on the first k characters considered so far.
        int[] rank = new int[n];

        // tempRank is used while recalculating ranks in each round.
        int[] tempRank = new int[n];

        // Initial ranks: just the character code at each position.
        for (int i = 0; i < n; i++) {
            suffixArray[i] = i;
            rank[i] = text.charAt(i);
        }

        // k = how many characters we currently compare (doubles each round)
        for (int k = 1; k < n; k = k * 2) {
            int[] currentRank = rank;

            // Sort suffixArray using rank pairs (rank[i], rank[i+k]) as the key.
            // We implement our own simple sort (merge sort) instead of using
            // library sort, to demonstrate our understanding of sorting.
            suffixArray = mergeSortByRankPair(suffixArray, currentRank, k, n);

            // Recalculate ranks based on the new order.
            tempRank[suffixArray[0]] = 0;
            for (int i = 1; i < n; i++) {
                int prev = suffixArray[i - 1];
                int curr = suffixArray[i];

                int prevSecond = secondKey(prev, k, n, currentRank);
                int currSecond = secondKey(curr, k, n, currentRank);

                boolean samePair = (currentRank[prev] == currentRank[curr]) && (prevSecond == currSecond);

                if (samePair) {
                    tempRank[curr] = tempRank[prev];
                } else {
                    tempRank[curr] = tempRank[prev] + 1;
                }
            }

            // Copy tempRank into rank for next round.
            for (int i = 0; i < n; i++) {
                rank[i] = tempRank[i];
            }

            // If all ranks are unique (last rank == n-1), suffixes are fully sorted.
            if (rank[suffixArray[n - 1]] == n - 1) {
                break;
            }
        }

        return suffixArray;
    }

    // Returns the "second key" (rank of the suffix starting k positions later)
    // used for comparing two suffixes during a doubling round.
    private static int secondKey(int index, int k, int n, int[] rank) {
        if (index + k < n) {
            return rank[index + k];
        }
        return -1; // treat "nothing there" as smaller than any real rank
    }

    // Simple merge sort that sorts suffix start-indices by the pair
    // (rank[index], secondKey(index)). This is our own sorting logic,
    // written explicitly instead of relying on library sort methods.
    private static int[] mergeSortByRankPair(int[] array, int[] rank, int k, int n) {
        if (array.length <= 1) {
            return array;
        }

        int mid = array.length / 2;
        int[] left = new int[mid];
        int[] right = new int[array.length - mid];

        for (int i = 0; i < mid; i++) {
            left[i] = array[i];
        }
        for (int i = mid; i < array.length; i++) {
            right[i - mid] = array[i];
        }

        left = mergeSortByRankPair(left, rank, k, n);
        right = mergeSortByRankPair(right, rank, k, n);

        return merge(left, right, rank, k, n);
    }

    // Merges two sorted arrays of suffix indices based on (rank, secondKey) pair.
    private static int[] merge(int[] left, int[] right, int[] rank, int k, int n) {
        int[] result = new int[left.length + right.length];
        int i = 0;
        int j = 0;
        int pos = 0;

        while (i < left.length && j < right.length) {
            int leftFirst = rank[left[i]];
            int rightFirst = rank[right[j]];

            int leftSecond = secondKey(left[i], k, n, rank);
            int rightSecond = secondKey(right[j], k, n, rank);

            boolean leftIsSmallerOrEqual;
            if (leftFirst != rightFirst) {
                leftIsSmallerOrEqual = leftFirst < rightFirst;
            } else {
                leftIsSmallerOrEqual = leftSecond <= rightSecond;
            }

            if (leftIsSmallerOrEqual) {
                result[pos] = left[i];
                i++;
            } else {
                result[pos] = right[j];
                j++;
            }
            pos++;
        }

        while (i < left.length) {
            result[pos] = left[i];
            i++;
            pos++;
        }
        while (j < right.length) {
            result[pos] = right[j];
            j++;
            pos++;
        }

        return result;
    }

    // Simple standalone test using the classic "banana" example.
    public static void main(String[] args) {
        String text = "banana";
        int[] sa = build(text);

        System.out.print("Suffix Array for \"" + text + "\": [");
        for (int i = 0; i < sa.length; i++) {
            System.out.print(sa[i]);
            if (i < sa.length - 1) {
                System.out.print(", ");
            }
        }
        System.out.println("]");
        System.out.println("Expected:               [5, 3, 1, 0, 4, 2]");

        System.out.println();
        System.out.println("Sorted suffixes:");
        for (int i = 0; i < sa.length; i++) {
            System.out.println("  " + sa[i] + ": " + text.substring(sa[i]));
        }
    }
}

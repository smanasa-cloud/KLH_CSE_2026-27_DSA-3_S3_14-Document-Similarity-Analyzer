package engine;

/*
 * KasaiLCP.java
 *
 * WHAT IS LCP?
 * LCP stands for "Longest Common Prefix". Given the Suffix Array (suffixes
 * sorted alphabetically), the LCP array tells us, for each position i in the
 * sorted order, how many starting characters are shared between the suffix
 * at SA[i] and the suffix at SA[i-1] (its neighbor just before it).
 *
 * Example for "banana":
 *   Sorted suffixes:      Suffix Array:   LCP:
 *   a                     5               0   (no suffix before it)
 *   ana                   3               1   (shares "a" with "a")
 *   anana                 1               3   (shares "ana" with "ana")
 *   banana                0               0   (nothing shared with "anana")
 *   na                    4               0   (nothing shared with "banana")
 *   nana                  2               2   (shares "na" with "na")
 *
 * WHY DO WE NEED THIS?
 * Neighboring suffixes in the sorted order are the "most similar" suffixes
 * to each other. The LCP value tells us exactly how much text they share.
 * When we later combine two documents into one string and build the SA+LCP,
 * a high LCP value between suffixes from DIFFERENT documents tells us those
 * documents share a common substring of that length.
 *
 * WHY KASAI'S ALGORITHM (AND NOT A NAIVE APPROACH)?
 * A naive approach would compare each pair of neighboring suffixes character
 * by character from scratch: O(n) per pair, O(n^2) total in the worst case.
 *
 * Kasai's Algorithm computes the whole LCP array in O(n) total, using one
 * clever observation:
 *   If the suffix starting at position i has LCP length h with its neighbor
 *   in sorted order, then the suffix starting at position i+1 (one character
 *   shorter) will have LCP length AT LEAST (h - 1) with ITS neighbor.
 *   So we never need to "restart" the comparison from zero - we only ever
 *   decrease h by at most 1 each step, so total character comparisons
 *   across the whole array stay O(n).
 *
 * We need a "rank" array (the inverse of the suffix array) to know, for a
 * given starting index, WHERE it sits in the sorted suffix array - this lets
 * us jump directly to its correct neighbor.
 *
 * TIME COMPLEXITY: O(n)
 * SPACE COMPLEXITY: O(n) for the rank array and the LCP array.
 */
public class KasaiLCP {

    // Builds the LCP array given the original text and its suffix array.
    // lcp[i] = length of common prefix between suffixArray[i] and suffixArray[i-1].
    // lcp[0] is always 0 because there is no suffix before the first one.
    public static int[] build(String text, int[] suffixArray) {
        int n = text.length();
        int[] lcp = new int[n];

        if (n == 0) {
            return lcp;
        }

        // rank[i] = position of the suffix starting at index i inside suffixArray.
        // This is the inverse of the suffix array.
        int[] rank = new int[n];
        for (int i = 0; i < n; i++) {
            rank[suffixArray[i]] = i;
        }

        // h = length of the common prefix carried over from the previous suffix.
        int h = 0;

        for (int i = 0; i < n; i++) {
            // Only process suffixes that are not the last in sorted order.
            if (rank[i] > 0) {
                // j = starting index of the suffix that comes right before
                // suffix i in the sorted suffix array.
                int j = suffixArray[rank[i] - 1];

                // Extend the match as far as possible, starting from length h
                // (we don't reset to 0 - this is the key trick of Kasai's algorithm).
                while (i + h < n && j + h < n && text.charAt(i + h) == text.charAt(j + h)) {
                    h++;
                }

                lcp[rank[i]] = h;

                // Decrease h by 1 for the next suffix (i+1), since it is one
                // character shorter and can lose at most 1 unit of overlap.
                if (h > 0) {
                    h--;
                }
            } else {
                // This suffix is the first in sorted order, so there's no
                // predecessor to compare it with.
                h = 0;
            }
        }

        return lcp;
    }

    // Simple standalone test using the classic "banana" example.
    public static void main(String[] args) {
        String text = "banana";
        int[] sa = SuffixArray.build(text);
        int[] lcp = KasaiLCP.build(text, sa);

        System.out.println("Text: " + text);
        System.out.println("Index | Suffix   | SA | LCP");
        for (int i = 0; i < sa.length; i++) {
            System.out.println("  " + i + "   | " + pad(text.substring(sa[i])) + " | " + sa[i] + "  | " + lcp[i]);
        }

        System.out.println();
        System.out.println("Expected LCP array: [0, 1, 3, 0, 0, 2]");
    }

    // Small helper just to line up the test output nicely.
    private static String pad(String s) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < 8) {
            sb.append(' ');
        }
        return sb.toString();
    }
}

package engine;

/*
 * LongestCommonSubstring.java
 *
 * GOAL:
 * Given two documents, find the longest piece of text that appears in BOTH
 * of them (an exact substring match).
 *
 * HOW WE USE SUFFIX ARRAY + LCP FOR THIS:
 * 1. Combine both documents into one string using a separator character
 *    that cannot appear in either document, e.g.:
 *
 *      combined = textA + "#" + textB
 *
 *    The separator (#) makes sure suffixes never "cross over" from one
 *    document's text into the other document's text and match by accident.
 *
 * 2. Build the Suffix Array of the combined string.
 *
 * 3. Build the LCP array (using Kasai's Algorithm) of the combined string.
 *
 * 4. Walk through the sorted suffixes. Whenever two NEIGHBORING suffixes in
 *    the sorted order come from DIFFERENT documents (one starts before the
 *    separator, the other starts after it), their LCP value tells us how
 *    many characters they share - i.e. a common substring of that length.
 *
 * 5. The biggest such LCP value (among cross-document neighbors only) is the
 *    Longest Common Substring length. We also record the exact substring.
 *
 * WHY THIS WORKS:
 * In a sorted list of suffixes, if two suffixes share a substring, they will
 * appear near each other (all suffixes starting with that substring are
 * grouped together after sorting). So we only ever need to compare adjacent
 * suffixes in the sorted order, not every possible pair - that's what makes
 * this approach efficient.
 *
 * TIME COMPLEXITY:
 * Suffix Array construction: ~O(n log^2 n)
 * LCP (Kasai): O(n)
 * Final scan for cross-document LCP: O(n)
 * So the overall cost is dominated by suffix array construction.
 *
 * SPACE COMPLEXITY: O(n) for suffix array + LCP array.
 */
public class LongestCommonSubstring {

    // A separator character that we assume never appears inside normal text.
    // Since our TextPreprocessor only keeps letters, digits and spaces,
    // any other character (like '#') is safe to use as a separator.
    public static final char SEPARATOR = '#';

    // Simple result holder for the longest common substring search.
    public static class Result {
        public String substring;
        public int length;

        public Result(String substring, int length) {
            this.substring = substring;
            this.length = length;
        }
    }

    // Finds the longest common substring between textA and textB.
    public static Result find(String textA, String textB) {
        String combined = textA + SEPARATOR + textB;
        int separatorIndex = textA.length(); // position of '#' in combined text

        int[] suffixArray = SuffixArray.build(combined);
        int[] lcp = KasaiLCP.build(combined, suffixArray);

        int bestLength = 0;
        int bestStart = -1;

        // Scan every adjacent pair in the sorted suffix array.
        for (int i = 1; i < suffixArray.length; i++) {
            int posA = suffixArray[i - 1];
            int posB = suffixArray[i];

            boolean firstFromDocA = posA < separatorIndex;
            boolean secondFromDocA = posB < separatorIndex;

            // We only care about suffixes coming from DIFFERENT documents.
            boolean differentDocuments = firstFromDocA != secondFromDocA;

            if (differentDocuments && lcp[i] > bestLength) {
                bestLength = lcp[i];
                bestStart = posA < posB ? posA : posB;
            }
        }

        if (bestLength == 0 || bestStart == -1) {
            return new Result("", 0);
        }

        String substring = combined.substring(bestStart, bestStart + bestLength);
        return new Result(substring, bestLength);
    }

    // Simple standalone test with two small strings sharing an obvious substring.
    public static void main(String[] args) {
        String docA = "machine learning algorithms";
        String docB = "machine learning applications";

        Result result = find(docA, docB);

        System.out.println("Document A: " + docA);
        System.out.println("Document B: " + docB);
        System.out.println("Longest Common Substring: \"" + result.substring + "\"");
        System.out.println("Length: " + result.length);

        // A second, simpler test.
        Result result2 = find("abcdef", "xxbcdxx");
        System.out.println();
        System.out.println("Test 2 -> LCS: \"" + result2.substring + "\" length: " + result2.length);
        System.out.println("Expected: \"bcd\" length 3");
    }
}

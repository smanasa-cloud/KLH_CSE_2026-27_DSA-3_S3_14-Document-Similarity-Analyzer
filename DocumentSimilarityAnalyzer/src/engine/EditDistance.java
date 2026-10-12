package engine;

/*
 * EditDistance.java
 *
 * WHAT IS EDIT DISTANCE?
 * The Edit Distance (also called Levenshtein Distance) between two strings
 * is the minimum number of single-character operations needed to transform
 * one string into the other. The three allowed operations are:
 *   1. Insertion    (add a character)
 *   2. Deletion     (remove a character)
 *   3. Substitution (replace one character with another)
 *
 * Example:
 *   "kitten" -> "sitting" requires 3 operations:
 *     kitten -> sitten (substitute k->s)
 *     sitten -> sittin (substitute e->i)
 *     sittin -> sitting (insert g)
 *   So editDistance("kitten", "sitting") = 3.
 *
 * WHY DO WE NEED THIS FOR DOCUMENT SIMILARITY?
 * The Suffix Array + LCP approach finds EXACT common substrings. But two
 * documents can be very similar even with small word changes (like
 * "transforming" vs "changing"). Edit Distance captures this "fuzzy"
 * similarity that exact substring matching might miss.
 *
 * HOW WE COMPUTE IT: WAGNER-FISCHER DYNAMIC PROGRAMMING
 * Let dp[i][j] = edit distance between the first i characters of textA and
 * the first j characters of textB.
 *
 * Base cases:
 *   dp[0][j] = j   (convert empty string to textB's first j chars = j inserts)
 *   dp[i][0] = i   (delete all i characters of textA)
 *
 * Recurrence:
 *   if textA.charAt(i-1) == textB.charAt(j-1):
 *       dp[i][j] = dp[i-1][j-1]                     (characters match, no cost)
 *   else:
 *       dp[i][j] = 1 + min(
 *                       dp[i-1][j],     // delete from textA
 *                       dp[i][j-1],     // insert into textA
 *                       dp[i-1][j-1]    // substitute
 *                   )
 *
 * MEMORY OPTIMIZATION:
 * A full dp table needs O(n*m) memory, which can be large for big documents.
 * But notice that row i only depends on row i-1. So we only need to keep
 * TWO rows (previous row and current row) in memory at once, instead of the
 * whole table. This reduces space from O(n*m) to O(min(n,m)).
 *
 * TIME COMPLEXITY: O(n * m)  -- we must fill every cell of the conceptual table.
 * SPACE COMPLEXITY: O(min(n,m)) using the two-row optimization.
 *
 * IMPORTANT HONESTY NOTE:
 * O(n*m) is expensive for very large documents (e.g. two 50,000 character
 * documents would need 2.5 billion operations). For this student prototype,
 * we accept this cost for normal-sized text documents. In a real production
 * system, edit distance would only be run on smaller candidate pairs, not on
 * huge documents. We are honest about this limitation.
 */
public class EditDistance {

    // Calculates the Levenshtein edit distance between textA and textB
    // using a memory-optimized DP with only two rows instead of a full table.
    public static int calculate(String textA, String textB) {
        int lengthA = textA.length();
        int lengthB = textB.length();

        // Make sure textB is the shorter one, so our rows use less memory.
        // (This doesn't change the answer since edit distance is symmetric.)
        if (lengthB > lengthA) {
            String temp = textA;
            textA = textB;
            textB = temp;
            int tempLen = lengthA;
            lengthA = lengthB;
            lengthB = tempLen;
        }

        // previousRow[j] = dp[i-1][j], currentRow[j] = dp[i][j]
        int[] previousRow = new int[lengthB + 1];
        int[] currentRow = new int[lengthB + 1];

        // Base case: transforming empty textA-prefix into textB's first j chars
        // requires j insertions.
        for (int j = 0; j <= lengthB; j++) {
            previousRow[j] = j;
        }

        for (int i = 1; i <= lengthA; i++) {
            // Base case for this row: deleting all i characters of textA.
            currentRow[0] = i;

            char charA = textA.charAt(i - 1);

            for (int j = 1; j <= lengthB; j++) {
                char charB = textB.charAt(j - 1);

                if (charA == charB) {
                    // Characters match - no extra cost, carry diagonal value.
                    currentRow[j] = previousRow[j - 1];
                } else {
                    int deleteCost = previousRow[j] + 1;      // delete from textA
                    int insertCost = currentRow[j - 1] + 1;   // insert into textA
                    int substituteCost = previousRow[j - 1] + 1; // substitute

                    int minCost = deleteCost;
                    if (insertCost < minCost) {
                        minCost = insertCost;
                    }
                    if (substituteCost < minCost) {
                        minCost = substituteCost;
                    }
                    currentRow[j] = minCost;
                }
            }

            // Move current row into previous row for the next iteration.
            // Swap references instead of copying array contents (faster).
            int[] tempRow = previousRow;
            previousRow = currentRow;
            currentRow = tempRow;
        }

        // After the loop, the final answer sits in previousRow because of
        // the last swap.
        return previousRow[lengthB];
    }

    // Simple standalone test with well-known examples.
    public static void main(String[] args) {
        int distance1 = calculate("kitten", "sitting");
        System.out.println("kitten -> sitting : " + distance1 + " (expected 3)");

        int distance2 = calculate("banana", "banana");
        System.out.println("banana -> banana : " + distance2 + " (expected 0)");

        int distance3 = calculate("", "abc");
        System.out.println("\"\" -> abc : " + distance3 + " (expected 3)");

        int distance4 = calculate(
            "artificial intelligence is changing technology",
            "artificial intelligence is transforming technology"
        );
        System.out.println("Sentence example distance: " + distance4);
    }
}

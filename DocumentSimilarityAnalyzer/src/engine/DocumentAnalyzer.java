package engine;

import model.Document;

/*
 * DocumentAnalyzer.java
 *
 * PURPOSE:
 * This class runs the FULL comparison pipeline for ONE pair of documents,
 * exactly as described in the project pipeline:
 *
 *   1. Normalize both documents        (already done in Document constructor)
 *   2. Build combined string           (done inside LongestCommonSubstring)
 *   3. Build suffix array              (done inside LongestCommonSubstring)
 *   4. Build LCP using Kasai           (done inside LongestCommonSubstring)
 *   5. Find longest common substring
 *   6. Calculate suffix similarity
 *   7. Calculate edit distance
 *   8. Calculate edit similarity
 *   9. Combine both scores (final similarity)
 *  10. Return ComparisonResult
 *
 * This class also decides pairwise comparisons for a whole list of documents
 * (n(n-1)/2 unique pairs, no duplicates, no self-comparisons).
 */
public class DocumentAnalyzer {

    // Maximum characters shown in the "Longest Common Substring" preview,
    // so we never dump a huge block of text onto the webpage.
    public static final int LCS_PREVIEW_LIMIT = 60;

    // Compares exactly two documents and returns a full ComparisonResult.
    public static ComparisonResult compare(Document docA, Document docB, double duplicateThreshold) {
        ComparisonResult result = new ComparisonResult(docA.fileName, docB.fileName);

        String textA = docA.normalizedText;
        String textB = docB.normalizedText;

        // Step 5: Find longest common substring using Suffix Array + Kasai LCP.
        LongestCommonSubstring.Result lcsResult = LongestCommonSubstring.find(textA, textB);

        result.longestCommonSubstring = makePreview(lcsResult.substring);
        result.longestCommonSubstringLength = lcsResult.length;

        // Step 6: Suffix similarity.
        result.suffixSimilarity = SimilarityCalculator.calculateSuffixSimilarity(
            lcsResult.length, textA.length(), textB.length()
        );

        // Step 7: Edit distance (Wagner-Fischer DP, memory-optimized).
        result.editDistance = EditDistance.calculate(textA, textB);

        // Step 8: Edit similarity.
        result.editSimilarity = SimilarityCalculator.calculateEditSimilarity(
            result.editDistance, textA.length(), textB.length()
        );

        // Step 9: Final similarity (weighted combination).
        result.finalSimilarity = SimilarityCalculator.calculateFinalSimilarity(
            result.suffixSimilarity, result.editSimilarity
        );

        // Classification + duplicate flag.
        result.classification = SimilarityCalculator.classify(result.finalSimilarity);
        result.duplicateFlag = result.finalSimilarity >= duplicateThreshold;

        return result;
    }

    // Compares every unique pair among a list of documents.
    // For n documents, this produces n(n-1)/2 comparisons (no self-pairs,
    // no repeated reverse pairs like B-A after A-B).
    public static ComparisonResult[] compareAll(Document[] documents, double duplicateThreshold) {
        int n = documents.length;
        int pairCount = (n * (n - 1)) / 2;
        ComparisonResult[] results = new ComparisonResult[pairCount];

        int index = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                results[index] = compare(documents[i], documents[j], duplicateThreshold);
                index++;
            }
        }

        return results;
    }

    // Trims the longest common substring down to a safe preview length,
    // so the webpage never has to display an extremely long string.
    private static String makePreview(String substring) {
        if (substring.length() <= LCS_PREVIEW_LIMIT) {
            return substring;
        }
        return substring.substring(0, LCS_PREVIEW_LIMIT) + "...";
    }

    // Simple standalone test with two related documents.
    public static void main(String[] args) {
        Document docA = new Document(0, "doc1.txt",
            "Artificial intelligence is changing technology.");
        Document docB = new Document(1, "doc2.txt",
            "Artificial intelligence is transforming technology.");

        ComparisonResult result = compare(docA, docB, 90.0);

        System.out.println("Comparing: " + result.document1Name + " vs " + result.document2Name);
        System.out.println("Longest Common Substring: \"" + result.longestCommonSubstring + "\"");
        System.out.println("LCS Length: " + result.longestCommonSubstringLength);
        System.out.println("Suffix Similarity: " + round(result.suffixSimilarity) + "%");
        System.out.println("Edit Distance: " + result.editDistance);
        System.out.println("Edit Similarity: " + round(result.editSimilarity) + "%");
        System.out.println("Final Similarity: " + round(result.finalSimilarity) + "%");
        System.out.println("Classification: " + result.classification);
        System.out.println("Possible Duplicate: " + result.duplicateFlag);
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

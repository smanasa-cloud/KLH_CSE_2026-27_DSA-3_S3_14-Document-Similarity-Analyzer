package engine;

/*
 * SimilarityCalculator.java
 *
 * PURPOSE:
 * Combine the results of our two similarity signals (Suffix Array based
 * common substring, and Levenshtein Edit Distance) into ONE final similarity
 * score, and classify that score into a simple human-readable category.
 *
 * THIS IS OUR OWN PROJECT-DEFINED METRIC.
 * There is no single "correct" universal formula for document similarity.
 * We designed a simple, explainable formula for this student prototype:
 *
 *   suffixSimilarity = (2 * longestCommonSubstringLength) / (lengthA + lengthB)
 *   editSimilarity    = 1 - (editDistance / max(lengthA, lengthB))
 *   finalSimilarity   = SUFFIX_WEIGHT * suffixSimilarity + EDIT_WEIGHT * editSimilarity
 *
 * All similarity values are expressed as PERCENTAGES (0 to 100).
 *
 * WHY 70% SUFFIX + 30% EDIT?
 * We treat the exact-substring-match signal (Suffix Array/LCP) as the main
 * indicator of similarity because it directly finds shared text, and we
 * treat Edit Distance as a secondary "fuzzy" adjustment. These weights are
 * simple constants that can easily be changed - they are NOT a scientifically
 * proven optimal ratio, just a reasonable, explainable choice for this
 * project.
 */
public class SimilarityCalculator {

    // Weights used to combine suffix similarity and edit similarity.
    // Easy to modify - must add up to 1.0 for the final score to stay in 0-100 range.
    public static final double SUFFIX_WEIGHT = 0.70;
    public static final double EDIT_WEIGHT = 0.30;

    // Similarity category thresholds (percentages). Easy to modify.
    public static final double THRESHOLD_POSSIBLE_DUPLICATE = 90.0;
    public static final double THRESHOLD_HIGHLY_SIMILAR = 75.0;
    public static final double THRESHOLD_MODERATELY_SIMILAR = 50.0;
    public static final double THRESHOLD_SLIGHTLY_SIMILAR = 25.0;

    // Calculates suffix-based similarity as a percentage (0 to 100).
    // Formula: (2 * commonLength) / (lengthA + lengthB) * 100
    public static double calculateSuffixSimilarity(int longestCommonSubstringLength, int lengthA, int lengthB) {
        if (lengthA + lengthB == 0) {
            return 0.0;
        }
        double ratio = (2.0 * longestCommonSubstringLength) / (lengthA + lengthB);
        return ratio * 100.0;
    }

    // Calculates edit-distance-based similarity as a percentage (0 to 100).
    // Formula: (1 - editDistance / max(lengthA, lengthB)) * 100
    public static double calculateEditSimilarity(int editDistance, int lengthA, int lengthB) {
        int maxLength = Math.max(lengthA, lengthB);
        if (maxLength == 0) {
            return 100.0; // both documents are empty - treat as identical
        }
        double ratio = 1.0 - ((double) editDistance / (double) maxLength);
        if (ratio < 0) {
            ratio = 0; // edit distance can theoretically exceed maxLength; clamp at 0
        }
        return ratio * 100.0;
    }

    // Combines suffix similarity and edit similarity into one final score.
    public static double calculateFinalSimilarity(double suffixSimilarity, double editSimilarity) {
        return (SUFFIX_WEIGHT * suffixSimilarity) + (EDIT_WEIGHT * editSimilarity);
    }

    // Classifies a final similarity percentage into a simple readable category.
    public static String classify(double finalSimilarity) {
        if (finalSimilarity >= THRESHOLD_POSSIBLE_DUPLICATE) {
            return "Possible Duplicate";
        } else if (finalSimilarity >= THRESHOLD_HIGHLY_SIMILAR) {
            return "Highly Similar";
        } else if (finalSimilarity >= THRESHOLD_MODERATELY_SIMILAR) {
            return "Moderately Similar";
        } else if (finalSimilarity >= THRESHOLD_SLIGHTLY_SIMILAR) {
            return "Slightly Similar";
        } else {
            return "Low Similarity";
        }
    }

    // Simple standalone test using values similar to our example documents.
    public static void main(String[] args) {
        int lcsLength = 18;
        int lengthA = 27;
        int lengthB = 30;
        int editDistance = 6;

        double suffixSim = calculateSuffixSimilarity(lcsLength, lengthA, lengthB);
        double editSim = calculateEditSimilarity(editDistance, lengthA, lengthB);
        double finalSim = calculateFinalSimilarity(suffixSim, editSim);
        String category = classify(finalSim);

        System.out.println("Suffix Similarity: " + round(suffixSim) + "%");
        System.out.println("Edit Similarity: " + round(editSim) + "%");
        System.out.println("Final Similarity: " + round(finalSim) + "%");
        System.out.println("Category: " + category);
    }

    // Small helper to round to 2 decimal places for readable test output.
    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

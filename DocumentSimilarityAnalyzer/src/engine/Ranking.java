package engine;

import model.Document;

/*
 * Ranking.java
 *
 * PURPOSE:
 * Given one "target" document and its similarity score against every other
 * document, produce a ranked list from MOST similar to LEAST similar.
 *
 * WHY MERGE SORT?
 * The course wants us to avoid relying on library sorting inside the core
 * DSA engine, so we implement our own Merge Sort - a simple, well known,
 * stable, O(n log n) sorting algorithm that is easy to explain in a viva.
 *
 * TIME COMPLEXITY: O(n log n)
 * SPACE COMPLEXITY: O(n) for the temporary arrays used during merging.
 */
public class Ranking {

    // Simple holder for one ranked entry: a document name and its score.
    public static class RankedEntry {
        public String documentName;
        public double similarity;

        public RankedEntry(String documentName, double similarity) {
            this.documentName = documentName;
            this.similarity = similarity;
        }
    }

    // Sorts ranked entries by similarity, HIGHEST first, using merge sort.
    public static RankedEntry[] sortDescending(RankedEntry[] entries) {
        if (entries.length <= 1) {
            return entries;
        }

        int mid = entries.length / 2;
        RankedEntry[] left = new RankedEntry[mid];
        RankedEntry[] right = new RankedEntry[entries.length - mid];

        for (int i = 0; i < mid; i++) {
            left[i] = entries[i];
        }
        for (int i = mid; i < entries.length; i++) {
            right[i - mid] = entries[i];
        }

        left = sortDescending(left);
        right = sortDescending(right);

        return merge(left, right);
    }

    // Merges two arrays already sorted in descending order.
    private static RankedEntry[] merge(RankedEntry[] left, RankedEntry[] right) {
        RankedEntry[] result = new RankedEntry[left.length + right.length];
        int i = 0;
        int j = 0;
        int pos = 0;

        while (i < left.length && j < right.length) {
            if (left[i].similarity >= right[j].similarity) {
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

    // Builds a ranking of documents most similar to "targetDocument",
    // using the full list of pairwise comparison results.
    public static RankedEntry[] rankSimilarTo(Document targetDocument, ComparisonResult[] allResults) {
        // Count how many comparisons involve the target document.
        int count = 0;
        for (int i = 0; i < allResults.length; i++) {
            if (involvesDocument(allResults[i], targetDocument.fileName)) {
                count++;
            }
        }

        RankedEntry[] entries = new RankedEntry[count];
        int index = 0;
        for (int i = 0; i < allResults.length; i++) {
            ComparisonResult r = allResults[i];
            if (involvesDocument(r, targetDocument.fileName)) {
                String otherName = r.document1Name.equals(targetDocument.fileName)
                    ? r.document2Name : r.document1Name;
                entries[index] = new RankedEntry(otherName, r.finalSimilarity);
                index++;
            }
        }

        return sortDescending(entries);
    }

    private static boolean involvesDocument(ComparisonResult r, String fileName) {
        return r.document1Name.equals(fileName) || r.document2Name.equals(fileName);
    }

    // Simple standalone test.
    public static void main(String[] args) {
        RankedEntry[] entries = new RankedEntry[] {
            new RankedEntry("Document_01.txt", 76.0),
            new RankedEntry("Document_02.txt", 94.0),
            new RankedEntry("Document_03.txt", 81.0),
            new RankedEntry("Document_05.txt", 88.0)
        };

        RankedEntry[] sorted = sortDescending(entries);

        System.out.println("Ranking (highest similarity first):");
        for (int i = 0; i < sorted.length; i++) {
            System.out.println((i + 1) + ". " + sorted[i].documentName + " -> " + sorted[i].similarity + "%");
        }
        System.out.println("Expected order: 02(94), 05(88), 03(81), 01(76)");
    }
}

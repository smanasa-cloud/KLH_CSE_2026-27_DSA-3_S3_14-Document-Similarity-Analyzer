package engine;

import model.Document;

/*
 * SimilarityMatrix.java
 *
 * PURPOSE:
 * Build a simple 2D matrix showing the final similarity percentage between
 * every pair of documents. The diagonal is always 100% because a document
 * is always identical to itself.
 *
 * Example for 4 documents:
 *
 *        D1    D2    D3    D4
 *  D1   100    82    31    55
 *  D2    82   100    41    65
 *  D3    31    41   100    22
 *  D4    55    65    22   100
 *
 * The matrix is symmetric: matrix[i][j] == matrix[j][i], because
 * similarity between A and B is the same as between B and A.
 */
public class SimilarityMatrix {

    // Builds the full n x n similarity matrix from the pairwise comparison
    // results. documents[] gives us the row/column order.
    public static double[][] build(Document[] documents, ComparisonResult[] allResults) {
        int n = documents.length;
        double[][] matrix = new double[n][n];

        // Diagonal is always 100% (a document compared to itself).
        for (int i = 0; i < n; i++) {
            matrix[i][i] = 100.0;
        }

        // Fill in every pairwise result into both (i,j) and (j,i).
        for (int r = 0; r < allResults.length; r++) {
            ComparisonResult result = allResults[r];

            int indexA = findIndexByName(documents, result.document1Name);
            int indexB = findIndexByName(documents, result.document2Name);

            if (indexA != -1 && indexB != -1) {
                matrix[indexA][indexB] = result.finalSimilarity;
                matrix[indexB][indexA] = result.finalSimilarity;
            }
        }

        return matrix;
    }

    private static int findIndexByName(Document[] documents, String fileName) {
        for (int i = 0; i < documents.length; i++) {
            if (documents[i].fileName.equals(fileName)) {
                return i;
            }
        }
        return -1;
    }

    // Simple standalone test with 3 tiny documents.
    public static void main(String[] args) {
        Document[] docs = new Document[] {
            new Document(0, "D1", "the cat sat on the mat"),
            new Document(1, "D2", "the cat sat on the mat"),
            new Document(2, "D3", "completely different text here")
        };

        ComparisonResult[] results = DocumentAnalyzer.compareAll(docs, 90.0);
        double[][] matrix = build(docs, results);

        System.out.println("Similarity Matrix:");
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                System.out.print(Math.round(matrix[i][j]) + "\t");
            }
            System.out.println();
        }
    }
}

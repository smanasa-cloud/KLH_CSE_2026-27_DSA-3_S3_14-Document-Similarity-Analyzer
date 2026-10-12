package engine;

import model.Document;

/*
 * Clustering.java
 *
 * PURPOSE:
 * Group documents into simple "clusters" (related groups) based on a
 * similarity threshold chosen by the user. This is NOT machine learning
 * clustering (no K-Means/DBSCAN) - it is a simple graph idea:
 *
 *   - Treat every document as a NODE.
 *   - Draw an EDGE between two documents if their final similarity is
 *     greater than or equal to the grouping threshold.
 *   - A CLUSTER is simply a CONNECTED COMPONENT of this graph.
 *
 * HOW WE FIND CONNECTED COMPONENTS:
 * We use a simple approach: for each document not yet visited, do a
 * Breadth-First Search (BFS) using our similarity matrix as the adjacency
 * information, and mark every document reachable from it as one cluster.
 *
 * WHY THIS IS SIMPLE AND EXPLAINABLE:
 * BFS/graph connectivity is a standard, well-known technique that any DSA
 * student can explain in a viva - no need for complicated ML clustering.
 *
 * TIME COMPLEXITY: O(n^2) in the worst case (n documents, checking all
 * pairs while exploring the graph) - acceptable for our small document sets.
 */
public class Clustering {

    // Builds clusters (groups of document names) using the similarity matrix
    // and a threshold. Returns an array of clusters, where each cluster is
    // an array of document file names.
    public static String[][] buildClusters(Document[] documents, double[][] matrix, double threshold) {
        int n = documents.length;
        boolean[] visited = new boolean[n];

        // Temporary storage: up to n clusters, each holding up to n names.
        String[][] tempClusters = new String[n][];
        int clusterCount = 0;

        for (int start = 0; start < n; start++) {
            if (visited[start]) {
                continue;
            }

            // BFS from "start" using a simple array-based queue.
            int[] queue = new int[n];
            int queueHead = 0;
            int queueTail = 0;

            queue[queueTail] = start;
            queueTail++;
            visited[start] = true;

            // Collect the members of this cluster as we go.
            String[] members = new String[n];
            int memberCount = 0;

            while (queueHead < queueTail) {
                int current = queue[queueHead];
                queueHead++;

                members[memberCount] = documents[current].fileName;
                memberCount++;

                // Check every other document for an "edge" (similarity >= threshold).
                for (int neighbor = 0; neighbor < n; neighbor++) {
                    if (!visited[neighbor] && neighbor != current && matrix[current][neighbor] >= threshold) {
                        visited[neighbor] = true;
                        queue[queueTail] = neighbor;
                        queueTail++;
                    }
                }
            }

            // Trim the members array down to the exact size used.
            String[] trimmedMembers = new String[memberCount];
            for (int i = 0; i < memberCount; i++) {
                trimmedMembers[i] = members[i];
            }

            tempClusters[clusterCount] = trimmedMembers;
            clusterCount++;
        }

        // Trim the clusters array down to the exact number of clusters found.
        String[][] clusters = new String[clusterCount][];
        for (int i = 0; i < clusterCount; i++) {
            clusters[i] = tempClusters[i];
        }

        return clusters;
    }

    // Simple standalone test with 4 tiny documents.
    public static void main(String[] args) {
        Document[] docs = new Document[] {
            new Document(0, "Doc1.txt", "the quick brown fox jumps over the lazy dog"),
            new Document(1, "Doc2.txt", "the quick brown fox jumps over the lazy dog"),
            new Document(2, "Doc3.txt", "completely unrelated text about cooking recipes"),
            new Document(3, "Doc4.txt", "totally different content about space travel")
        };

        ComparisonResult[] results = DocumentAnalyzer.compareAll(docs, 90.0);
        double[][] matrix = SimilarityMatrix.build(docs, results);

        String[][] clusters = buildClusters(docs, matrix, 80.0);

        System.out.println("Clusters (threshold 80%):");
        for (int i = 0; i < clusters.length; i++) {
            System.out.print("Cluster " + (i + 1) + ": ");
            for (int j = 0; j < clusters[i].length; j++) {
                System.out.print(clusters[i][j]);
                if (j < clusters[i].length - 1) {
                    System.out.print(", ");
                }
            }
            System.out.println();
        }
    }
}

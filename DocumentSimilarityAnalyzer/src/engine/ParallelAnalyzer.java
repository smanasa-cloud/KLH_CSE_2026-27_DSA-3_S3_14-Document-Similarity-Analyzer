package engine;

import model.Document;

/*
 * ParallelAnalyzer.java
 *
 * PURPOSE (MODULE 6 - SMALL DEMONSTRATION OF PARALLEL PROCESSING):
 * When comparing many documents, each pairwise comparison (A-B, A-C, B-C...)
 * is INDEPENDENT of the others - comparing A with B does not need the result
 * of comparing A with C. This means we can run several comparisons AT THE
 * SAME TIME on different CPU threads, instead of one after another.
 *
 * SEQUENTIAL MODE:
 *   One comparison finishes completely before the next one starts.
 *   This is what DocumentAnalyzer.compareAll() already does.
 *
 * PARALLEL MODE:
 *   We split the list of pairs into a few groups, and give each group to
 *   its own Thread. All threads run at approximately the same time, and we
 *   wait (using join()) for all of them to finish before returning results.
 *
 * WE KEEP THIS EXTREMELY SIMPLE:
 *   - Plain java.lang.Thread (no ExecutorService, no thread pools).
 *   - A fixed small number of worker threads.
 *   - Each thread writes its results into its own slice of the results
 *     array, so there is no need for locks or synchronization.
 *
 * WHY NO SYNCHRONIZATION IS NEEDED:
 * Every thread only writes to indexes it "owns" (indices assigned only to
 * that thread). No two threads ever touch the same array slot, so there is
 * no race condition, even without using synchronized blocks.
 */
public class ParallelAnalyzer {

    // Number of worker threads to use in parallel mode. Simple fixed value.
    public static final int THREAD_COUNT = 4;

    // A small Runnable task: compares a specific range of document pairs
    // and stores each result directly into its assigned slot in results[].
    private static class CompareTask implements Runnable {
        private Document[] documents;
        private int[] pairIndexA;
        private int[] pairIndexB;
        private int startPair;
        private int endPair; // exclusive
        private ComparisonResult[] results;
        private double duplicateThreshold;

        CompareTask(Document[] documents, int[] pairIndexA, int[] pairIndexB,
                    int startPair, int endPair, ComparisonResult[] results, double duplicateThreshold) {
            this.documents = documents;
            this.pairIndexA = pairIndexA;
            this.pairIndexB = pairIndexB;
            this.startPair = startPair;
            this.endPair = endPair;
            this.results = results;
            this.duplicateThreshold = duplicateThreshold;
        }

        public void run() {
            for (int p = startPair; p < endPair; p++) {
                int a = pairIndexA[p];
                int b = pairIndexB[p];
                results[p] = DocumentAnalyzer.compare(documents[a], documents[b], duplicateThreshold);
            }
        }
    }

    // Runs all pairwise comparisons using multiple threads.
    public static ComparisonResult[] compareAllParallel(Document[] documents, double duplicateThreshold) {
        int n = documents.length;
        int pairCount = (n * (n - 1)) / 2;

        int[] pairIndexA = new int[pairCount];
        int[] pairIndexB = new int[pairCount];
        int index = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                pairIndexA[index] = i;
                pairIndexB[index] = j;
                index++;
            }
        }

        ComparisonResult[] results = new ComparisonResult[pairCount];

        int threadsToUse = THREAD_COUNT;
        if (pairCount < threadsToUse) {
            threadsToUse = pairCount;
        }
        if (threadsToUse < 1) {
            threadsToUse = 1;
        }

        Thread[] threads = new Thread[threadsToUse];
        int chunkSize = (pairCount + threadsToUse - 1) / threadsToUse;

        for (int t = 0; t < threadsToUse; t++) {
            int start = t * chunkSize;
            int end = Math.min(start + chunkSize, pairCount);

            if (start >= end) {
                threads[t] = null;
                continue;
            }

            CompareTask task = new CompareTask(documents, pairIndexA, pairIndexB, start, end, results, duplicateThreshold);
            threads[t] = new Thread(task);
            threads[t].start();
        }

        for (int t = 0; t < threadsToUse; t++) {
            if (threads[t] != null) {
                try {
                    threads[t].join();
                } catch (InterruptedException e) {
                    System.out.println("Thread was interrupted: " + e.getMessage());
                }
            }
        }

        return results;
    }

    // Simple standalone test comparing sequential vs parallel timing and results.
    public static void main(String[] args) {
        Document[] docs = new Document[] {
            new Document(0, "Doc1.txt", "the quick brown fox jumps over the lazy dog"),
            new Document(1, "Doc2.txt", "the quick brown fox jumps over the lazy dog"),
            new Document(2, "Doc3.txt", "completely unrelated text about cooking recipes"),
            new Document(3, "Doc4.txt", "totally different content about space travel"),
            new Document(4, "Doc5.txt", "the quick brown fox jumps over the sleepy dog")
        };

        long startSeq = System.nanoTime();
        ComparisonResult[] sequentialResults = DocumentAnalyzer.compareAll(docs, 90.0);
        long endSeq = System.nanoTime();

        long startPar = System.nanoTime();
        ComparisonResult[] parallelResults = compareAllParallel(docs, 90.0);
        long endPar = System.nanoTime();

        System.out.println("Sequential comparisons: " + sequentialResults.length
            + " took " + (endSeq - startSeq) / 1000000.0 + " ms");
        System.out.println("Parallel comparisons: " + parallelResults.length
            + " took " + (endPar - startPar) / 1000000.0 + " ms");

        System.out.println();
        System.out.println("Verifying both modes produce the same results:");
        boolean allMatch = true;
        for (int i = 0; i < sequentialResults.length; i++) {
            double diff = Math.abs(sequentialResults[i].finalSimilarity - parallelResults[i].finalSimilarity);
            if (diff > 0.0001) {
                allMatch = false;
            }
        }
        System.out.println(allMatch ? "MATCH - sequential and parallel results are identical." : "MISMATCH found!");
    }
}

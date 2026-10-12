# Viva questions — short answers

**What is document similarity?** A numerical estimate of how much two documents share. Our score combines one exact matching passage with edit similarity.

**What is a suffix?** A suffix is the text from any starting position to the end. For `banana`, `ana` is a suffix starting at index 3.

**Why a suffix array?** It sorts suffix starting positions so related text appears next to related text. This lets us identify a long shared substring without checking all possible substring pairs.

**What is LCP?** Longest Common Prefix: how many starting characters two neighboring sorted suffixes share.

**What does Kasai do? Why O(n)?** It builds the LCP array from a suffix array. It reuses the previous match length, decreasing it by at most one each step, so its total work is linear.

**What is the longest common substring?** The longest consecutive exact character sequence present in both normalized documents. It may be in the middle of either document.

**What is edit distance?** The minimum number of insertions, deletions, or substitutions needed to turn one text into another. Our Java class uses two DP rows.

**Why O(a×b) for edit distance?** Dynamic programming computes a value for each pair of text prefixes, making a grid with a×b cells.

**How is the final score calculated?** 70% of the longest shared substring score plus 30% of edit similarity. The weights were chosen for an easy educational demo, not learned or universally optimal.

**What is the threshold?** A pair at or above the selected percentage is flagged as a possible duplicate and forms a grouping link. It does not change the pair's score.

**Why is the matrix symmetric?** Comparing A with B gives the same score as B with A. The diagonal is 100% because a document matches itself.

**How many pairs are compared?** For d documents, d(d−1)/2 unique unordered pairs.

**What is a group?** A connected component of the threshold graph. Membership can be indirect through a third document.

**Where are Modules 1, 2, 3, and 6?** Module 1: architecture and algorithm selection. Module 2: suffix array and LCP. Module 3: edit distance DP. Module 6: optional threads for independent pairs.

**Why not Modules 4 and 5?** Network flow and NP-hard optimization are not needed for this text comparison pipeline.

**Why Java and no machine learning?** This is a DSA demonstration: the algorithms are implemented directly in Java and can be explained step by step.

**What are the limitations?** The suffix score counts only one longest shared passage, edit distance is slow for long texts, and this version handles at most 10 short TXT/DOCX documents. Word extraction reads the main document text, while PDF and old DOC formats are unsupported. Normalization keeps only ASCII letters, digits, and spaces.

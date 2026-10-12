package engine;

/*
 * TextPreprocessor.java
 *
 * PURPOSE:
 * Before we can compare two documents using Suffix Arrays or Edit Distance,
 * we must "normalize" the text so that small formatting differences
 * (capital letters, extra spaces, punctuation) do not affect the result.
 *
 * This class only uses simple char arrays and loops - no java.util needed,
 * because normalization is part of our DSA engine and should be understandable
 * step by step.
 *
 * STEPS PERFORMED:
 * 1. Convert all letters to lowercase.
 * 2. Remove punctuation characters (keep only letters, digits and spaces).
 * 3. Collapse multiple spaces/tabs/newlines into a single space.
 * 4. Trim leading and trailing spaces.
 */
public class TextPreprocessor {

    // Main method that other classes will call.
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }

        // Step 1: lowercase
        String lower = text.toLowerCase();

        // Step 2: remove punctuation (keep letters, digits, whitespace)
        char[] chars = lower.toCharArray();
        char[] cleaned = new char[chars.length];
        int cleanedLength = 0;

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            boolean isLetter = (c >= 'a' && c <= 'z');
            boolean isDigit = (c >= '0' && c <= '9');
            boolean isWhitespace = (c == ' ' || c == '\t' || c == '\n' || c == '\r');

            if (isLetter || isDigit || isWhitespace) {
                cleaned[cleanedLength] = c;
                cleanedLength++;
            } else {
                // Punctuation like . , ! ? " ' etc. is replaced with a space
                // so words don't accidentally merge together.
                cleaned[cleanedLength] = ' ';
                cleanedLength++;
            }
        }

        // Step 3: collapse repeated whitespace into single spaces
        char[] collapsed = new char[cleanedLength];
        int collapsedLength = 0;
        boolean previousWasSpace = false;

        for (int i = 0; i < cleanedLength; i++) {
            char c = cleaned[i];
            boolean isWhitespace = (c == ' ' || c == '\t' || c == '\n' || c == '\r');

            if (isWhitespace) {
                if (!previousWasSpace) {
                    collapsed[collapsedLength] = ' ';
                    collapsedLength++;
                }
                previousWasSpace = true;
            } else {
                collapsed[collapsedLength] = c;
                collapsedLength++;
                previousWasSpace = false;
            }
        }

        // Step 4: trim leading/trailing spaces
        int start = 0;
        int end = collapsedLength - 1;

        while (start <= end && collapsed[start] == ' ') {
            start++;
        }
        while (end >= start && collapsed[end] == ' ') {
            end--;
        }

        if (end < start) {
            return "";
        }

        return new String(collapsed, start, end - start + 1);
    }

    // Simple standalone test so we can verify this class works on its own.
    public static void main(String[] args) {
        String sample = "  Hello,   WORLD!!  This... is a TEST.\n\nNew line here.  ";
        String result = normalize(sample);

        System.out.println("Original: [" + sample + "]");
        System.out.println("Normalized: [" + result + "]");
    }
}

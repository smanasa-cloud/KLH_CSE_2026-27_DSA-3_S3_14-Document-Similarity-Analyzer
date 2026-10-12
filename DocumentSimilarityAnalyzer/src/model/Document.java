package model;

import engine.TextPreprocessor;

/** A user document and the normalized text used by the algorithms. */
public class Document {
    public final int id;
    public final String fileName;
    public final String originalText;
    public final String normalizedText;

    public Document(int id, String fileName, String originalText) {
        this.id = id;
        this.fileName = fileName;
        this.originalText = originalText;
        this.normalizedText = TextPreprocessor.normalize(originalText);
    }
}

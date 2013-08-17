package com.twitter;

import java.text.Normalizer;

/**
 * A class for validating Tweet texts.
 */
public class Validator {
    private static final int MAX_TWEET_LENGTH = 140;

    private int shortUrlLength = 22;
    private int shortUrlLengthHttps = 23;

    private final Extractor extractor = new Extractor();

    public int getTweetLength(String text) {
        text = Normalizer.normalize(text, Normalizer.Form.NFC);
        int length = text.codePointCount(0, text.length());

        for (Extractor.Entity urlEntity : extractor
                .extractURLsWithIndices(text)) {
            length += urlEntity.start - urlEntity.end;
            length += urlEntity.value.toLowerCase().startsWith("https://") ? shortUrlLengthHttps
                    : shortUrlLength;
        }

        return length;
    }

    public boolean isValidTweet(String text) {
        return isValidTweet(text, MAX_TWEET_LENGTH);
    }

    public boolean isValidTweet(String text, int maxLength) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        for (char c : text.toCharArray()) {
            if (c == '\uFFFE' || c == '\uuFEFF' ||   // BOM
                    c == '\uFFFF' ||                     // Special
                    (c >= '\u202A' && c <= '\u202E')) {  // Direction change
                return false;
            }
        }

        return getTweetLength(text) <= maxLength;
    }
}

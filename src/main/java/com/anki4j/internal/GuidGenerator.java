package com.anki4j.internal;

import java.security.SecureRandom;

/**
 * Generates Anki-compatible GUIDs (10 characters).
 */
public final class GuidGenerator {
    private static final String CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!#$%&()*+,-./:;<=>?@[]^_`{|}~";
    private static final SecureRandom RANDOM = new SecureRandom();

    private GuidGenerator() {
    }

    /**
     * Generates a random 10-character GUID.
     * 
     * @return A random string of 10 characters from the Anki alphabet.
     */
    public static String generate() {
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}

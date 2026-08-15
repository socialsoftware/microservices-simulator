package pt.ulisboa.tecnico.socialsoftware.consistencytesting.utils;

import java.io.File;
import java.nio.file.Path;

public class StringUtils {

    private StringUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Converts a positive integer to its ordinal representation (e.g., 1 -> "1st",
     * 2 -> "2nd", 3 -> "3rd", 4 -> "4th").
     *
     * @param i The positive integer to convert to ordinal format
     * @return The ordinal representation of the input integer
     * @throws IllegalArgumentException If the input {@code i} is less than or equal
     *                                  to zero
     */
    public static String ordinal(int i) {
        if (i <= 0) {
            throw new IllegalArgumentException("Ordinal numbers must be greater than zero. Received: " + i);
        }

        String[] suffixes = new String[] { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };

        switch (i % 100) {
            case 11:
            case 12:
            case 13:
                return i + "th";
            default:
                return i + suffixes[i % 10];
        }
    }

    /** Longest file name segment {@link #toFileNameSafe} emits. */
    private static final int MAX_FILE_NAME_LENGTH = 80;

    /**
     * Rewrites {@code name} into a single path segment safe on every filesystem:
     * every character outside {@code [A-Za-z0-9._-]} becomes {@code '_'} and the
     * result is truncated to {@value #MAX_FILE_NAME_LENGTH} characters.
     * <p>
     * Truncation can make two long names collide; callers that need uniqueness
     * (report directories, for instance) must not rely on this alone.
     */
    public static String toFileNameSafe(String name) {
        String sanitized = name.replaceAll("[^A-Za-z0-9._-]", "_");
        return sanitized.length() <= MAX_FILE_NAME_LENGTH
                ? sanitized
                : sanitized.substring(0, MAX_FILE_NAME_LENGTH);
    }

    /**
     * {@code path} as text with {@code '/'} separators whatever the platform
     * that produced it, e.g. {@code reports\run-1.json} becomes
     * {@code reports/run-1.json} on Windows.
     */
    public static String toPortableString(Path path) {
        return path.toString().replace(File.separatorChar, '/');
    }
}

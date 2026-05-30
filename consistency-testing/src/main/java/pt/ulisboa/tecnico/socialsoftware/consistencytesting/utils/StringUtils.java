package pt.ulisboa.tecnico.socialsoftware.consistencytesting.utils;

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
}

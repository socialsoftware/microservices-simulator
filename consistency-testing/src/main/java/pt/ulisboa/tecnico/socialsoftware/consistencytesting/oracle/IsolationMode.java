package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.Arrays;
import java.util.Locale;

/**
 * Whether an oracle owns every resource used by an application-under-test.
 * <p>
 * Isolation is the safe default. Applications that intentionally use an
 * external or singleton resource must opt out explicitly; the tool then makes
 * no claim that concurrent campaigns are safe.
 */
public enum IsolationMode {
    REQUIRED,
    UNSUPPORTED;

    public static final String PROPERTY = "consistency.isolation";

    public static IsolationMode fromSystemProperty() {
        String value = System.getProperty(PROPERTY, "required");
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "required" -> REQUIRED;
            case "unsupported" -> UNSUPPORTED;
            default -> throw new IllegalArgumentException(
                    "System property '%s' must be one of %s, got '%s'"
                            .formatted(PROPERTY, acceptedValues(), value));
        };
    }

    private static String acceptedValues() {
        return Arrays.stream(values())
                .map(mode -> mode.name().toLowerCase(Locale.ROOT))
                .toList()
                .toString();
    }
}

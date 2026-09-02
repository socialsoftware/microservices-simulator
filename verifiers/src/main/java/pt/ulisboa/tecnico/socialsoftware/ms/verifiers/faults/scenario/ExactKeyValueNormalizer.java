package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Pattern;

/** Conservative value normalization shared by exact graph and input evidence matching. */
final class ExactKeyValueNormalizer {

    private static final Pattern INTEGRAL = Pattern.compile("[+-]?[0-9][0-9_]*[lL]?");
    private static final Pattern DECIMAL = Pattern.compile(
            "[+-]?(?:(?:[0-9][0-9_]*\\.[0-9_]*)|(?:\\.[0-9][0-9_]*))(?:[eE][+-]?[0-9][0-9_]*)?[fFdD]?");
    private static final Pattern EXPONENT = Pattern.compile(
            "[+-]?[0-9][0-9_]*[eE][+-]?[0-9][0-9_]*[fFdD]?");

    private ExactKeyValueNormalizer() {
    }

    static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        try {
            if (INTEGRAL.matcher(value).matches()) {
                String numeric = value.replace("_", "");
                if (numeric.endsWith("l") || numeric.endsWith("L")) {
                    numeric = numeric.substring(0, numeric.length() - 1);
                }
                return "number:" + new BigInteger(numeric);
            }
            if (DECIMAL.matcher(value).matches() || EXPONENT.matcher(value).matches()) {
                String numeric = value.replace("_", "");
                char suffix = numeric.charAt(numeric.length() - 1);
                if (suffix == 'f' || suffix == 'F' || suffix == 'd' || suffix == 'D') {
                    numeric = numeric.substring(0, numeric.length() - 1);
                }
                BigDecimal decimal = new BigDecimal(numeric).stripTrailingZeros();
                return "number:" + decimal.toPlainString();
            }
        } catch (NumberFormatException ignored) {
            // Preserve unusual or out-of-range spellings conservatively below.
        }
        return "text:" + value;
    }
}

package pt.ulisboa.tecnico.socialsoftware.consistencytesting.utils;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ArgsUtils {

    /**
     * Merges two lists of arguments, giving precedence to high-priority arguments.
     * <p>
     * If a low-priority argument shares the same property key as a high-priority
     * argument,
     * the low-priority argument is omitted from the final list. The resulting list
     * preserves the order, placing all high-priority arguments first, followed by
     * the non-conflicting low-priority arguments.
     *
     * @param highPriorityArgs the list of high-priority arguments.
     * @param lowPriorityArgs  the list of low-priority arguments.
     * @return A new list containing the merged arguments.
     */
    public static List<String> mergeArgsWithPriority(List<String> highPriorityArgs, List<String> lowPriorityArgs) {
        // Regex to capture the property of the argument
        // (e.g., "--spring.profiles.active=sagas,local,test"),
        // returns the string between "--" and "=" (or end of string)
        Pattern argPropertyPattern = Pattern.compile("^--([^=]+)");

        Function<String, String> getArgProperty = arg -> {
            Matcher matcher = argPropertyPattern.matcher(arg);
            return matcher.find() ? matcher.group(1) : null;
        };

        Set<String> highPriorityProperties = highPriorityArgs.stream()
                .map(getArgProperty)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Function<String, Boolean> argDoesNotOverwriteHighPriorityArg = arg -> {
            return !highPriorityProperties.contains(getArgProperty.apply(arg));
        };

        List<String> nonConflitctingLowPriorityArgs = lowPriorityArgs.stream()
                .filter(argDoesNotOverwriteHighPriorityArg::apply)
                .toList();

        return Stream.concat(highPriorityArgs.stream(), nonConflitctingLowPriorityArgs.stream()).toList();
    }
}

package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.Set;
import java.util.function.Supplier;

public record InterInvariant(
                String name,
                Supplier<Set<InterInvariantViolation>> predicate) {
}

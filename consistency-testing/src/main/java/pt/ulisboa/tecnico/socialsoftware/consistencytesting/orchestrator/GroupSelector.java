package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

/**
 * Exact selection of one planned group, written as {@code catalog/group-label}.
 */
record GroupSelector(String catalog, String group) {

    private static final char SEPARATOR = '/';

    GroupSelector {
        if (catalog == null || catalog.isBlank() || group == null || group.isBlank()) {
            throw new IllegalArgumentException("Group selector catalog and group must be non-blank");
        }
    }

    /**
     * Parses the canonical {@code <catalog>/<group-label>} selector format.
     *
     * @throws IllegalArgumentException if the value is null or does not contain
     *                                  exactly one non-edge separator
     *                                  ('{@value #SEPARATOR}').
     */
    static GroupSelector parse(String value) {
        if (value == null) {
            throw invalidSelector("null");
        }

        String selector = value.strip();
        int separator = selector.indexOf(SEPARATOR);
        if (separator <= 0
                || separator == selector.length() - 1
                || selector.indexOf(SEPARATOR, separator + 1) >= 0) {
            // must have a single separator, not at the start or end
            throw invalidSelector(value);
        }

        return new GroupSelector(
                selector.substring(0, separator).strip(),
                selector.substring(separator + 1).strip());
    }

    boolean matches(String catalogName, String groupLabel) {
        return catalog.equals(catalogName) && group.equals(groupLabel);
    }

    String toSelector() {
        return catalog + SEPARATOR + group;
    }

    private static IllegalArgumentException invalidSelector(String value) {
        return new IllegalArgumentException(
                "Group selector must have form '<catalog>/<group-label>', got: '" + value + "'");
    }
}

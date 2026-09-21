package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

/** One scheduler choice, expressed without any concrete step identity. */
public record ScheduleDecision(int readySetSize, int selectedIndex) {

    public ScheduleDecision {
        if (readySetSize < 1) {
            throw new IllegalArgumentException("readySetSize must be >= 1, got " + readySetSize);
        }
        if (selectedIndex < 0 || selectedIndex >= readySetSize) {
            throw new IllegalArgumentException(
                    "selectedIndex must be within the ready set, got %d for size %d"
                            .formatted(selectedIndex, readySetSize));
        }
    }
}

package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario;

public record RecoveryScheduleCap(int value) {

    public static final int DEFAULT_VALUE = 20;

    public RecoveryScheduleCap {
        if (value <= 0) {
            throw new IllegalArgumentException("recovery schedule cap must be a positive integer");
        }
    }

    public static RecoveryScheduleCap defaultCap() {
        return new RecoveryScheduleCap(DEFAULT_VALUE);
    }

    public static RecoveryScheduleCap parse(String configuredValue) {
        if (configuredValue == null) {
            return defaultCap();
        }
        if (configuredValue.isBlank()) {
            throw new IllegalArgumentException("recovery schedule cap must be a positive integer");
        }
        try {
            return new RecoveryScheduleCap(Integer.parseInt(configuredValue.trim()));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("recovery schedule cap must be a positive integer");
        }
    }
}

package pt.ulisboa.tecnico.socialsoftware.ms.versioning;

/**
 * Snowflake ID generator for distributed version IDs.
 *
 * ID layout (64 bits):
 * 1 bit unused | 41 bits timestamp | 10 bits machine ID | 12 bits sequence
 *
 * - Timestamp: milliseconds since custom epoch (2024-01-01), gives ~69 years
 * - Machine ID: 0-1023, configurable per node
 * - Sequence: 0-4095 per millisecond per node
 */
public class SnowflakeIdGenerator {

    // Custom epoch: 2024-01-01T00:00:00Z
    private static final long CUSTOM_EPOCH = 1704067200000L;

    private static final int MACHINE_ID_BITS = 10;
    private static final int SEQUENCE_BITS = 12;

    private static final long MAX_MACHINE_ID = (1L << MACHINE_ID_BITS) - 1; // 1023
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1; // 4095

    private static final int MACHINE_ID_SHIFT = SEQUENCE_BITS;
    private static final int TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;

    private final long machineId;
    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public SnowflakeIdGenerator(long machineId) {
        if (machineId < 0 || machineId > MAX_MACHINE_ID) {
            throw new IllegalArgumentException(
                    "Machine ID must be between 0 and " + MAX_MACHINE_ID + ", got: " + machineId);
        }
        this.machineId = machineId;
    }

    public synchronized long nextId() {
        long currentTimestamp = currentTimeMillis();

        if (currentTimestamp < lastTimestamp) {
            throw new IllegalStateException(
                    "Clock moved backwards. Refusing to generate ID for " + (lastTimestamp - currentTimestamp)
                            + " milliseconds.");
        }

        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                // Sequence exhausted for this millisecond, wait for next
                currentTimestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0;
        }

        lastTimestamp = currentTimestamp;

        return ((currentTimestamp - CUSTOM_EPOCH) << TIMESTAMP_SHIFT)
                | (machineId << MACHINE_ID_SHIFT)
                | sequence;
    }

    private long waitNextMillis(long lastTimestamp) {
        long timestamp = currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTimeMillis();
        }
        return timestamp;
    }

    protected long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public long getMachineId() {
        return machineId;
    }
}

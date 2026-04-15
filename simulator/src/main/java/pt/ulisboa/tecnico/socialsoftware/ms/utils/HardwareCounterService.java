package pt.ulisboa.tecnico.socialsoftware.ms.utils;

import jnr.ffi.LibraryLoader;
import jnr.ffi.Pointer;
import jnr.ffi.Struct;
import jnr.ffi.types.pid_t;

public class HardwareCounterService {
    private static final HardwareCounterService instance = new HardwareCounterService();

    private final LibC libc;
    private final boolean available;

    private static final int PERF_TYPE_HARDWARE = 0;
    private static final int PERF_COUNT_HW_CPU_CYCLES = 0;
    private static final int PERF_COUNT_HW_INSTRUCTIONS = 1;

    private static final int SYS_perf_event_open;
    private static final int SYS_ioctl;
    private static final int SYS_read;

    static {
        // * Define syscall numbers
        String arch = System.getProperty("os.arch").toLowerCase();
        if (arch.contains("aarch64") || arch.contains("arm64")) {
            SYS_perf_event_open = 241;
            SYS_ioctl = 29;
            SYS_read = 63;
        } else {
            // Default to x86_64
            SYS_perf_event_open = 298;
            SYS_ioctl = 16;
            SYS_read = 0;
        }
    }

    // * Thread-local storage:
    // * Each thread can only access its own ThreadCounter instance with get()
    private static final ThreadLocal<ThreadCounters> threadCounters = ThreadLocal.withInitial(() -> null);

    public interface LibC {
        // * Interface providing method "syscall" needed to invoke "perf_event_open"
        int syscall(int number, Object... args);

        @pid_t
        long gettid();
    }

    private static class PerfEventAttr extends Struct {
        // * Represents C's structure "perf_event_attr"
        // * perf_event_open requires a pointer to a block of memory
        // * formatted exaclty like this in order to provide it
        // * with a complex set of configuration flags
        public final Unsigned32 type = new Unsigned32();
        public final Unsigned32 size = new Unsigned32();
        public final Unsigned64 config = new Unsigned64();
        public final Unsigned64 sample_period = new Unsigned64();
        public final Unsigned64 sample_type = new Unsigned64();
        public final Unsigned64 read_format = new Unsigned64();
        public final Unsigned64 flags = new Unsigned64();
        // Additional fields to reach at least PERF_ATTR_SIZE_VER0 (64 bytes)
        public final Unsigned32 wakeup_events = new Unsigned32();
        public final Unsigned32 bp_type = new Unsigned32();
        public final Unsigned64 config1 = new Unsigned64();
        public final Unsigned64 config2 = new Unsigned64();

        public PerfEventAttr(jnr.ffi.Runtime runtime) {
            super(runtime);
            size.set(Struct.size(this));
        }
    }

    private static class ThreadCounters {
        // * Data-container for hardware metrics from a specific thread
        final int instructionsFd;
        final int cyclesFd;
        final Pointer buffer;

        ThreadCounters(int instructionsFd, int cyclesFd, Pointer buffer) {
            this.instructionsFd = instructionsFd;
            this.cyclesFd = cyclesFd;
            this.buffer = buffer;
        }
    }

    // --- Set up ---

    private HardwareCounterService() {
        LibC libcLocal = null;
        boolean availableLocal = false;
        try {
            if (System.getProperty("os.name").toLowerCase().contains("linux")) {
                // * Inject LibC with C's library in order to call "syscall"
                libcLocal = LibraryLoader.create(LibC.class).load("c");
                availableLocal = true;
            }
        } catch (Throwable t) {
            System.err.println("HardwareCounterService: Failed to load libc: " + t.getMessage());
            availableLocal = false;
        }
        this.libc = libcLocal;
        this.available = availableLocal;
    }

    public static HardwareCounterService getInstance() {
        return instance;
    }

    // --- Private Helpers ---

    private int openPerfEvent(int config) {
        // * This method is responsible for requesting a specific
        // * Hardware Counter from the Linux Kernel
        PerfEventAttr attr = new PerfEventAttr(jnr.ffi.Runtime.getRuntime(libc));
        attr.type.set(PERF_TYPE_HARDWARE);
        attr.config.set(config);

        // * Flags
        // * 1 -> start counter in disabled mode
        // * 2 -> pinned slot (No guesses, either returns real value or error)
        // * 32 -> exclude_kernel (don't count OS work)
        // * 64 -> exclude_hv (don't count VM overhead)
        // * 128 -> ignore idle time (maybe unnecessary for local reading)
        // attr.flags.set(1 | 32 | 64);
        attr.flags.set(1 | 2 | 32 | 64 | 128);

        // * Parameters
        // * 0 -> Monitor the current Thread only
        // * -1 -> Ignore CPU, follow the Thread wherever it goes
        // * ... (the rest doesn't really matter)
        int fd = libc.syscall(SYS_perf_event_open, Struct.getMemory(attr), 0, -1, -1, 0);

        if (fd >= 0) {
            // * ioctl to enable: Start Counter Now
            libc.syscall(SYS_ioctl, fd, 0x2400, 0);
        } else {
            System.err.println("HardwareCounterService: perf_event_open failed with " + fd + " for config " + config);
        }
        return fd;
    }

    private long readFd(int fd, Pointer buffer) {
        // * Standard Linux read operation on a file descriptor
        int res = libc.syscall(SYS_read, fd, buffer, 8);
        if (res == 8) {
            return buffer.getLong(0);
        }
        return 0;
    }

    // --- Utility Methods ---

    public boolean isAvailable() {
        return available;
    }

    public void startThreadCounters() {
        // * Start counting hardware instructions for this specific Thread
        if (!available)
            return;

        if (threadCounters.get() != null)
            // Only count once per thread life-time
            return;

        // Request counters for both instructions and cycles
        int instructionsFd = openPerfEvent(PERF_COUNT_HW_INSTRUCTIONS);
        int cyclesFd = openPerfEvent(PERF_COUNT_HW_CPU_CYCLES);

        // Store them in a ThreadLocal Container
        if (instructionsFd >= 0 && cyclesFd >= 0) {
            // Alocate off-head space to ensure JAVA's GC doesn't move it around
            Pointer buffer = jnr.ffi.Runtime.getRuntime(libc).getMemoryManager().allocateDirect(8);
            threadCounters.set(new ThreadCounters(instructionsFd, cyclesFd, buffer));
        } else {
            System.err.println("HardwareCounterService: Failed to open perf events for thread " + libc.gettid());
        }
    }

    public long[] readValues() {
        // * Read hardware metrics measured from this specific Thread until now
        if (!available)
            return null;

        ThreadCounters counters = threadCounters.get();
        if (counters == null) {
            startThreadCounters();
            counters = threadCounters.get();
        }

        if (counters == null)
            return null;

        long instructions = readFd(counters.instructionsFd, counters.buffer);
        long cycles = readFd(counters.cyclesFd, counters.buffer);

        // If either read fails, return null to avoid invalid deltas
        if (instructions <= 0 || cycles <= 0)
            return null;

        return new long[] { instructions, cycles };
    }
}

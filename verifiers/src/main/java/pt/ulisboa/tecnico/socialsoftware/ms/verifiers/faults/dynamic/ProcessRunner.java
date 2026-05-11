package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public interface ProcessRunner {
    ProcessResult run(ProcessCommand command) throws IOException, InterruptedException;

    record ProcessCommand(Path workingDirectory, List<String> arguments, Duration timeout) {
        public ProcessCommand {
            if (workingDirectory == null) {
                throw new IllegalArgumentException("workingDirectory cannot be null");
            }
            arguments = arguments == null ? List.of() : List.copyOf(arguments);
            if (arguments.isEmpty()) {
                throw new IllegalArgumentException("arguments cannot be empty");
            }
            timeout = timeout == null ? Duration.ZERO : timeout;
        }
    }

    record ProcessResult(int exitCode, String stdout, String stderr, boolean timedOut) {
        public ProcessResult {
            stdout = stdout == null ? "" : stdout;
            stderr = stderr == null ? "" : stderr;
        }
    }
}

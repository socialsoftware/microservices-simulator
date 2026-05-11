package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic;

import java.io.IOException;

@FunctionalInterface
interface ProcessFactory {
    Process start(ProcessRunner.ProcessCommand command) throws IOException;
}

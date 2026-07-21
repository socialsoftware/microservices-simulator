package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;

public final class OnDemandFaultScenarioLockProcess {

    private OnDemandFaultScenarioLockProcess() {
    }

    public static void main(String[] args) throws Exception {
        Path manifestPath = Path.of(args[0]);
        String workloadPlanId = args[1];
        String vector = args[2];
        Path readyPath = Path.of(args[3]);
        Path acquiredPath = Path.of(args[4]);
        Path completedPath = Path.of(args[5]);
        Path resultPath = Path.of(args[6]);

        OnDemandFaultScenarioService.NioPackageLockProvider nioProvider =
                new OnDemandFaultScenarioService.NioPackageLockProvider(
                        ignored -> Files.writeString(readyPath, "ready\n"));
        OnDemandFaultScenarioService.PackageLockProvider signalingProvider = lockPath -> {
            OnDemandFaultScenarioService.PackageLockHandle delegate = nioProvider.open(lockPath);
            return new OnDemandFaultScenarioService.PackageLockHandle() {
                @Override
                public void acquire() throws java.io.IOException {
                    delegate.acquire();
                    Files.writeString(acquiredPath, "acquired\n");
                }

                @Override
                public void close() throws java.io.IOException {
                    delegate.close();
                }
            };
        };

        OnDemandFaultScenarioResult result = new OnDemandFaultScenarioService(signalingProvider).request(
                new OnDemandFaultScenarioRequest(manifestPath, workloadPlanId, vector, "2"));
        Files.writeString(resultPath, new ObjectMapper().writeValueAsString(result));
        Files.writeString(completedPath, "completed\n");
        System.exit(result.successful() ? 0 : 1);
    }
}

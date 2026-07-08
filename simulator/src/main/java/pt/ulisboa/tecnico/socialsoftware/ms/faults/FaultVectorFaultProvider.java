package pt.ulisboa.tecnico.socialsoftware.ms.faults;

import java.util.Optional;

@FunctionalInterface
public interface FaultVectorFaultProvider {
    Optional<FaultVectorFault> faultFor(FaultVectorBoundaryContext context);
}

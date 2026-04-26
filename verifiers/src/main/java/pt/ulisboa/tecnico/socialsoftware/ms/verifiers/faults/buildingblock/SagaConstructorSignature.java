package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock;

import java.util.List;

public record SagaConstructorSignature(List<String> parameterTypeFqns) {

    public SagaConstructorSignature {
        parameterTypeFqns = parameterTypeFqns == null ? List.of() : List.copyOf(parameterTypeFqns);
    }
}

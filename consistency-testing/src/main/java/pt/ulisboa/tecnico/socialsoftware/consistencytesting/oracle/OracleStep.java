package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.Set;

interface OracleStep {
    void execute();

    StepId getId();

    FunctionalityId getFunctionalityId();

    Set<StepId> getDependencies();
}
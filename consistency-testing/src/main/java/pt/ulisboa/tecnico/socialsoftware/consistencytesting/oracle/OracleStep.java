package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.Set;

sealed interface OracleStep
        permits FunctionalityStep, CompensationStep, CommitStep, AbortStep, EventHandlerStep {

    void execute();

    StepId getId();

    FunctionalityId getFunctionalityId();

    Set<StepId> getDependencies();
}
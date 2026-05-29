package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.Set;

public interface OracleStep {
    public void execute();

    public StepId getId();

    public FunctionalityId getFunctionalityId();

    public Set<StepId> getDependencies();
}
package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.DynamicEvidenceJoinStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioPlan;

record CatalogScenarioRecord(ScenarioPlan plan,
                             DynamicEvidenceJoinStatus joinStatus,
                             int lineNumber,
                             String catalogKind,
                             String catalogPath) {
}

package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state

import spock.lang.Specification

class SourceModeClassificationSpec extends Specification {

    def 'default classification is unknown with empty evidence'() {
        when:
        def classification = SourceModeClassification.unknown()

        then:
        classification.sourceMode() == SourceMode.UNKNOWN
        classification.confidence() == SourceModeConfidence.UNKNOWN
        classification.evidence().isEmpty()
        classification.isUnknown()
        classification.warningText() != null
        classification.warningText().contains('unknown')
        classification.rejectionReason() == null
    }

    def 'classification defensively copies evidence and supports rejection reasons'() {
        given:
        def evidence = ['local test configuration', 'active profile']

        when:
        def classification = new SourceModeClassification(
                SourceMode.SAGAS,
                SourceModeConfidence.TEST_CONFIGURATION,
                evidence,
                SourceModeRejectionReason.SOURCE_MODE_TCC_REJECTED_FOR_SAGA_CATALOG
        )
        evidence << 'mutated'

        then:
        classification.sourceMode() == SourceMode.SAGAS
        classification.confidence() == SourceModeConfidence.TEST_CONFIGURATION
        classification.evidence() == ['local test configuration', 'active profile']
        classification.rejectionReason() == SourceModeRejectionReason.SOURCE_MODE_TCC_REJECTED_FOR_SAGA_CATALOG
        !classification.isUnknown()
    }

    def 'classifier defaults to unknown classification'() {
        when:
        def classification = new SourceModeClassifier().classify(null)

        then:
        classification == SourceModeClassification.unknown()
    }
}

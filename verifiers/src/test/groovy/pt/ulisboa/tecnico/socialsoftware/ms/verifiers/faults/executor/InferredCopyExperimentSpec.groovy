package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor

import groovy.json.JsonSlurper
import spock.lang.Requires
import spock.lang.Specification
import spock.lang.Unroll

/** Opt-in research tests, run by experiments/inferred-stale-write/qualify.py. */
@Requires({ sys['inferred.copy.proof'] })
class InferredCopyExperimentSpec extends Specification {
    File root = new File(System.getProperty('inferred.copy.proof'))
    def read(String path) { new JsonSlurper().parse(new File(root, path)) }

    def 'infers renamed fields from dummyapp and excludes a computed replacement'() {
        when:
        def contracts = read('fixture-contracts.json')
        then:
        contracts.size() == 1
        contracts[0].sourceType.endsWith('.CardInput')
        contracts[0].targetType.endsWith('.StoredCard')
        contracts[0].fields == [aggregateId: 'reference', caption: 'heading']
        contracts[0].proof.every { it.line > 0 }
    }

    @Unroll
    def 'runtime origin survives #mode'() {
        given:
        def trace = read("controls/${mode}.json")
        def copies = trace.events.findAll { it.kind == 'CONSTRUCTOR_COPY' && it.data.key == 1 }
        expect:
        trace.gaps.empty
        copies.size() == 1
        copies[0].data.values.heading == 'old'
        trace.events.any { it.order == copies[0].data.readOrder && it.kind == 'RESPONSE' }
        where:
        mode << ['same-instance', 'transport-clone', 'transport-reorder', 'transport-alias']
    }

    @Unroll
    def 'does not attribute the old caption for #mode'() {
        given:
        def trace = read("controls/${mode}.json")
        expect:
        trace.gaps.empty
        !trace.events.any { it.kind == 'CONSTRUCTOR_COPY' && it.data.key == 1 && it.data.values.containsKey('heading') }
        where:
        mode << ['cloned-untracked', 'changed-input']
    }

    @Unroll
    def 'fails closed when transport identity is not justified: #mode'() {
        given:
        def trace = read("controls/${mode}.json")
        expect:
        !trace.gaps.empty
        !trace.events.any { it.kind == 'CONSTRUCTOR_COPY' }
        where:
        mode << ['transport-mismatch', 'duplicate-key']
    }

    def 'constructing a copy does not certify persistence'() {
        given:
        def trace = read('controls/ignored-construction.json')
        expect:
        trace.events.any { it.kind == 'CONSTRUCTOR_COPY' }
        !trace.events.any { it.kind in ['REGISTERED_COPY', 'COMMITTED_WRITE'] }
    }
}

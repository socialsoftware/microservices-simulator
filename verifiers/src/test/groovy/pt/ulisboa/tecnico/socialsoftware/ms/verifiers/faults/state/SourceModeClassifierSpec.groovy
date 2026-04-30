package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state

import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

class SourceModeClassifierSpec extends Specification {

    @TempDir
    Path tempDir

    def 'classifies local autowired saga unit of work as SAGAS type evidence'() {
        given:
        writeSource('demo/SagaSpec.groovy', '''
            package demo
            import org.springframework.beans.factory.annotation.Autowired
            import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWorkService
            class SagaSpec {
                @Autowired private SagaUnitOfWorkService unitOfWorkService
            }
        ''')

        when:
        def classification = classify('demo.SagaSpec')

        then:
        classification.sourceMode() == SourceMode.SAGAS
        classification.confidence() == SourceModeConfidence.TYPE_EVIDENCE
        classification.evidence().any { it.contains('SagaUnitOfWorkService') }
    }

    def 'classifies local autowired causal unit of work as TCC type evidence'() {
        given:
        writeSource('demo/CausalSpec.groovy', '''
            package demo
            import org.springframework.beans.factory.annotation.Autowired
            import pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.unitOfWork.CausalUnitOfWorkService
            class CausalSpec {
                @Autowired private CausalUnitOfWorkService unitOfWorkService
            }
        ''')

        expect:
        classify('demo.CausalSpec').sourceMode() == SourceMode.TCC
        classify('demo.CausalSpec').confidence() == SourceModeConfidence.TYPE_EVIDENCE
    }

    def 'classifies nested test configuration bean returning saga unit of work as SAGAS test configuration'() {
        given:
        writeSource('demo/SagaConfigSpec.groovy', '''
            package demo
            import org.springframework.boot.test.context.TestConfiguration
            import org.springframework.context.annotation.Bean
            import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWorkService
            class SagaBeanConfig {
                @Bean SagaUnitOfWorkService unitOfWorkService() { new SagaUnitOfWorkService() }
            }
            class SagaConfigSpec {
                @TestConfiguration static class LocalConfig extends SagaBeanConfig {}
            }
        ''')

        when:
        def classification = classify('demo.SagaConfigSpec')

        then:
        classification.sourceMode() == SourceMode.SAGAS
        classification.confidence() == SourceModeConfidence.TEST_CONFIGURATION
        classification.evidence().any { it.contains('LocalConfig') && it.contains('SagaUnitOfWorkService') }
    }

    def 'classifies nested test configuration bean returning causal unit of work as TCC test configuration'() {
        given:
        writeSource('demo/CausalConfigSpec.groovy', '''
            package demo
            import org.springframework.boot.test.context.TestConfiguration
            import org.springframework.context.annotation.Bean
            import pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.unitOfWork.CausalUnitOfWorkService
            class CausalBeanConfig {
                @Bean CausalUnitOfWorkService unitOfWorkService() { new CausalUnitOfWorkService() }
            }
            class CausalConfigSpec {
                @TestConfiguration static class LocalConfig extends CausalBeanConfig {}
            }
        ''')

        expect:
        classify('demo.CausalConfigSpec').sourceMode() == SourceMode.TCC
        classify('demo.CausalConfigSpec').confidence() == SourceModeConfidence.TEST_CONFIGURATION
    }

    def 'classifies broad bean return type by constructed saga unit of work'() {
        given:
        writeSource('demo/BroadConfigSpec.groovy', '''
            package demo
            import org.springframework.boot.test.context.TestConfiguration
            import org.springframework.context.annotation.Bean
            import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWorkService
            class BroadBeanConfig {
                @Bean Object unitOfWorkService() { return new SagaUnitOfWorkService() }
            }
            class BroadConfigSpec {
                @TestConfiguration static class LocalConfig extends BroadBeanConfig {}
            }
        ''')

        expect:
        classify('demo.BroadConfigSpec').sourceMode() == SourceMode.SAGAS
        classify('demo.BroadConfigSpec').confidence() == SourceModeConfidence.TEST_CONFIGURATION
    }

    def 'classifies explicit active profiles'() {
        given:
        writeSource('demo/ProfileSpecs.groovy', '''
            package demo
            import org.springframework.test.context.ActiveProfiles
            @ActiveProfiles("sagas") class SagasProfileSpec {}
            @ActiveProfiles(["tcc"]) class TccProfileSpec {}
        ''')

        expect:
        classify('demo.SagasProfileSpec').sourceMode() == SourceMode.SAGAS
        classify('demo.SagasProfileSpec').confidence() == SourceModeConfidence.ACTIVE_PROFILE
        classify('demo.TccProfileSpec').sourceMode() == SourceMode.TCC
        classify('demo.TccProfileSpec').confidence() == SourceModeConfidence.ACTIVE_PROFILE
    }

    def 'does not guess unsupported profile aliases'() {
        given:
        writeSource('demo/ProfileAliasSpec.groovy', '''
            package demo
            import org.springframework.test.context.ActiveProfiles
            @ActiveProfiles("saga") class SagaAliasSpec {}
            @ActiveProfiles("causal") class CausalAliasSpec {}
        ''')

        expect:
        classify('demo.SagaAliasSpec').sourceMode() == SourceMode.UNKNOWN
        classify('demo.CausalAliasSpec').sourceMode() == SourceMode.UNKNOWN
    }

    def 'classifies explicit spring profile properties'() {
        given:
        writeSource('demo/PropertySpecs.groovy', '''
            package demo
            import org.springframework.boot.test.context.SpringBootTest
            import org.springframework.test.context.TestPropertySource
            @TestPropertySource(properties = "spring.profiles.active=sagas") class SagasPropertySpec {}
            @SpringBootTest(properties = "spring.profiles.active=tcc") class TccPropertySpec {}
        ''')

        expect:
        classify('demo.SagasPropertySpec').sourceMode() == SourceMode.SAGAS
        classify('demo.SagasPropertySpec').confidence() == SourceModeConfidence.ACTIVE_PROFILE
        classify('demo.TccPropertySpec').sourceMode() == SourceMode.TCC
        classify('demo.TccPropertySpec').confidence() == SourceModeConfidence.ACTIVE_PROFILE
    }

    def 'classifies conflicting same strength type evidence as MIXED'() {
        given:
        writeSource('demo/MixedTypeSpec.groovy', '''
            package demo
            import org.springframework.beans.factory.annotation.Autowired
            import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWorkService
            import pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.unitOfWork.CausalUnitOfWorkService
            class MixedTypeSpec {
                @Autowired private SagaUnitOfWorkService sagaUnitOfWorkService
                @Autowired private CausalUnitOfWorkService causalUnitOfWorkService
            }
        ''')

        expect:
        classify('demo.MixedTypeSpec').sourceMode() == SourceMode.MIXED
    }

    def 'classifies conflicting active profile and test configuration evidence as MIXED'() {
        given:
        writeSource('demo/MixedStrongSpec.groovy', '''
            package demo
            import org.springframework.boot.test.context.TestConfiguration
            import org.springframework.context.annotation.Bean
            import org.springframework.test.context.ActiveProfiles
            import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWorkService
            @ActiveProfiles("tcc")
            class MixedStrongSpec {
                @TestConfiguration static class LocalConfig extends SagaBeanConfig {}
            }
            class SagaBeanConfig {
                @Bean SagaUnitOfWorkService unitOfWorkService() { new SagaUnitOfWorkService() }
            }
        ''')

        expect:
        classify('demo.MixedStrongSpec').sourceMode() == SourceMode.MIXED
    }

    def 'classifies no evidence as UNKNOWN'() {
        given:
        writeSource('demo/UnknownSpec.groovy', '''
            package demo
            class UnknownSpec {}
        ''')

        expect:
        classify('demo.UnknownSpec') == SourceModeClassification.unknown()
    }

    def 'local causal evidence dominates inherited optional saga evidence'() {
        given:
        writeSource('demo/InheritedSpec.groovy', '''
            package demo
            import org.springframework.beans.factory.annotation.Autowired
            import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWorkService
            import pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.unitOfWork.CausalUnitOfWorkService
            class BaseSpec {
                @Autowired(required = false) protected SagaUnitOfWorkService unitOfWorkService
            }
            class ConcreteSpec extends BaseSpec {
                @Autowired private CausalUnitOfWorkService causalUnitOfWorkService
            }
        ''')

        expect:
        classify('demo.ConcreteSpec').sourceMode() == SourceMode.TCC
        classify('demo.ConcreteSpec').confidence() == SourceModeConfidence.TYPE_EVIDENCE
    }

    def 'inherited optional autowired field does not suppress later inherited evidence'() {
        given:
        writeSource('demo/InheritedOrderingSpec.groovy', '''
            package demo
            import org.springframework.beans.factory.annotation.Autowired
            import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWorkService
            import pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.unitOfWork.CausalUnitOfWorkService
            class BaseSpec {
                @Autowired(required = false) protected SagaUnitOfWorkService optionalSaga
                @Autowired protected CausalUnitOfWorkService causalUnitOfWorkService
            }
            class ConcreteSpec extends BaseSpec {}
        ''')

        expect:
        classify('demo.ConcreteSpec').sourceMode() == SourceMode.TCC
        classify('demo.ConcreteSpec').confidence() == SourceModeConfidence.TYPE_EVIDENCE
    }

    private SourceModeClassification classify(String classFqn) {
        def index = new GroovySourceIndex()
        index.parse(tempDir)
        return new SourceModeClassifier(index.classesByFqn, index.sourceBackedSuperclassByClassFqn)
                .classify(index.classesByFqn[classFqn])
    }

    private Path writeSource(String relativePath, String contents) {
        def file = tempDir.resolve(relativePath)
        Files.createDirectories(file.parent)
        Files.writeString(file, contents.stripIndent().trim() + '\n')
        return file
    }
}

package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state

import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

class GroovySourceIndexSpec extends Specification {

    @TempDir
    Path tempDir

    def 'parse indexes package, imports, and source-backed superclass relationships'() {
        given:
        writeSource('demo/base/BaseSpec.groovy', '''
            package demo.base
            class BaseSpec {}
        ''')
        writeSource('demo/feature/FeatureSpec.groovy', '''
            package demo.feature
            import demo.base.BaseSpec
            import java.time.Clock

            class FeatureSpec extends BaseSpec {}
        ''')

        def index = new GroovySourceIndex()

        when:
        index.parse(tempDir)

        then:
        index.classesByFqn.keySet() == [
            'demo.base.BaseSpec',
            'demo.feature.FeatureSpec'
        ] as Set

        with(index.classesByFqn['demo.feature.FeatureSpec']) {
            packageName == 'demo.feature'
            imports*.importedType as Set == ['demo.base.BaseSpec', 'java.time.Clock'] as Set
        }

        index.sourceBackedSuperclassByClassFqn['demo.feature.FeatureSpec'] == 'demo.base.BaseSpec'
    }

    def 'parse leaves external superclasses unresolved'() {
        given:
        writeSource('demo/external/ExternalChildSpec.groovy', '''
            package demo.external
            import java.util.ArrayList

            class ExternalChildSpec extends ArrayList {}
        ''')

        def index = new GroovySourceIndex()

        when:
        index.parse(tempDir)

        then:
        index.classesByFqn['demo.external.ExternalChildSpec'].packageName == 'demo.external'
        index.classesByFqn['demo.external.ExternalChildSpec'].imports*.importedType == ['java.util.ArrayList']
        !index.sourceBackedSuperclassByClassFqn.containsKey('demo.external.ExternalChildSpec')
    }

    def 'parse captures autowired unit-of-work fields and annotation attributes'() {
        given:
        writeSource('demo/SourceModeSpec.groovy', '''
            package demo

            import org.springframework.beans.factory.annotation.Autowired
            import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWorkService
            import pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.unitOfWork.CausalUnitOfWorkService

            class SourceModeSpec {
                @Autowired(required = false)
                private SagaUnitOfWorkService sagaUnitOfWorkService

                @Autowired
                private CausalUnitOfWorkService causalUnitOfWorkService
            }
        ''')

        def index = new GroovySourceIndex()

        when:
        index.parse(tempDir)

        then:
        def metadata = index.classesByFqn['demo.SourceModeSpec']
        metadata.fields*.name as Set == ['sagaUnitOfWorkService', 'causalUnitOfWorkService'] as Set

        def sagaField = metadata.fields.find { it.name == 'sagaUnitOfWorkService' }
        sagaField.typeName == 'pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWorkService'
        sagaField.annotations*.name == ['org.springframework.beans.factory.annotation.Autowired']
        sagaField.annotations[0].attributes['required'] == false

        def causalField = metadata.fields.find { it.name == 'causalUnitOfWorkService' }
        causalField.typeName == 'pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.unitOfWork.CausalUnitOfWorkService'
        causalField.annotations*.name == ['org.springframework.beans.factory.annotation.Autowired']
        !causalField.annotations[0].attributes.containsKey('required')
    }

    def 'parse captures nested test configuration metadata'() {
        given:
        writeSource('demo/ConfigSpec.groovy', '''
            package demo

            import org.springframework.boot.test.context.TestConfiguration

            class BaseConfig {}

            class ConfigSpec {
                @TestConfiguration
                static class LocalConfig extends BaseConfig {}
            }
        ''')

        def index = new GroovySourceIndex()

        when:
        index.parse(tempDir)

        then:
        def nested = index.classesByFqn['demo.ConfigSpec$LocalConfig']
        nested.enclosingClassFqn == 'demo.ConfigSpec'
        nested.staticClass
        nested.declaredSuperclassName == 'BaseConfig'
        nested.annotations*.name == ['org.springframework.boot.test.context.TestConfiguration']
    }

    def 'parse captures active profiles annotations'() {
        given:
        writeSource('demo/ProfileSpecs.groovy', '''
            package demo

            import org.springframework.test.context.ActiveProfiles

            @ActiveProfiles("sagas")
            class SagasProfileSpec {}

            @ActiveProfiles(["tcc"])
            class TccProfileSpec {}
        ''')

        def index = new GroovySourceIndex()

        when:
        index.parse(tempDir)

        then:
        index.classesByFqn['demo.SagasProfileSpec'].annotations[0].attributes['value'] == 'sagas'
        index.classesByFqn['demo.TccProfileSpec'].annotations[0].attributes['value'] == ['tcc']
    }

    def 'parse captures Spring test profile properties'() {
        given:
        writeSource('demo/PropertySpecs.groovy', '''
            package demo

            import org.springframework.boot.test.context.SpringBootTest
            import org.springframework.test.context.TestPropertySource

            @TestPropertySource(properties = "spring.profiles.active=sagas")
            class TestPropertyProfileSpec {}

            @SpringBootTest(properties = "spring.profiles.active=tcc")
            class SpringBootProfileSpec {}
        ''')

        def index = new GroovySourceIndex()

        when:
        index.parse(tempDir)

        then:
        index.classesByFqn['demo.TestPropertyProfileSpec'].annotations[0].attributes['properties'] == 'spring.profiles.active=sagas'
        index.classesByFqn['demo.SpringBootProfileSpec'].annotations[0].attributes['properties'] == 'spring.profiles.active=tcc'
    }

    private Path writeSource(String relativePath, String contents) {
        def file = tempDir.resolve(relativePath)
        Files.createDirectories(file.parent)
        Files.writeString(file, contents.stripIndent().trim() + '\n')
        return file
    }
}

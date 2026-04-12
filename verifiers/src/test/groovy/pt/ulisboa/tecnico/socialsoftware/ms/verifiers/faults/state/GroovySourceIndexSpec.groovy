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

    private Path writeSource(String relativePath, String contents) {
        def file = tempDir.resolve(relativePath)
        Files.createDirectories(file.parent)
        Files.writeString(file, contents.stripIndent().trim() + '\n')
        return file
    }
}

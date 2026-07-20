package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic

import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

class DynamicEnrichmentTestDiscoverySpec extends Specification {

    @TempDir
    Path tempDir

    def 'empty include directories select all test classes under test source root'() {
        given:
        def applicationPath = tempDir.resolve('app')
        writeTestSource(applicationPath, 'src/test/groovy/com/example/AlphaSpec.groovy', '''
            package com.example
            class AlphaSpec {}
        ''')
        writeTestSource(applicationPath, 'src/test/groovy/com/example/sub/BetaTest.java', '''
            package com.example.sub;
            public class BetaTest {}
        ''')
        writeTestSource(applicationPath, 'src/test/groovy/pt/ulisboa/demo/GammaTest.groovy', '''
            package pt.ulisboa.demo
            class GammaTest {}
        ''')

        when:
        def discovered = new DynamicEnrichmentTestClassDiscoveryService().discover(
                applicationPath,
                dynamicConfig(includeDirs: [], excludeDirs: [], excludeClasses: [])
        )

        then:
        discovered == [
                'com.example.AlphaSpec',
                'com.example.sub.BetaTest',
                'pt.ulisboa.demo.GammaTest'
        ]
    }

    def 'include directories restrict selected test classes'() {
        given:
        def applicationPath = tempDir.resolve('app')
        writeTestSource(applicationPath, 'src/test/groovy/quizzes/sagas/behaviour/CreateTournamentTest.groovy', '''
            package quizzes.sagas.behaviour
            class CreateTournamentTest {}
        ''')
        writeTestSource(applicationPath, 'src/test/groovy/quizzes/causal/behaviour/CreateTournamentCausalTest.groovy', '''
            package quizzes.causal.behaviour
            class CreateTournamentCausalTest {}
        ''')

        when:
        def discovered = new DynamicEnrichmentTestClassDiscoveryService().discover(
                applicationPath,
                dynamicConfig(includeDirs: ['quizzes/sagas'], excludeDirs: [], excludeClasses: [])
        )

        then:
        discovered == ['quizzes.sagas.behaviour.CreateTournamentTest']
    }

    def 'exclude directories override include directories'() {
        given:
        def applicationPath = tempDir.resolve('app')
        writeTestSource(applicationPath, 'src/test/groovy/quizzes/sagas/behaviour/KeepMeTest.groovy', '''
            package quizzes.sagas.behaviour
            class KeepMeTest {}
        ''')
        writeTestSource(applicationPath, 'src/test/groovy/quizzes/sagas/internal/DropMeTest.groovy', '''
            package quizzes.sagas.internal
            class DropMeTest {}
        ''')

        when:
        def discovered = new DynamicEnrichmentTestClassDiscoveryService().discover(
                applicationPath,
                dynamicConfig(includeDirs: ['quizzes/sagas'], excludeDirs: ['quizzes/sagas/internal'], excludeClasses: [])
        )

        then:
        discovered == ['quizzes.sagas.behaviour.KeepMeTest']
    }

    def 'exclude test classes remove dynamic evidence self-tests by simple name and FQN'() {
        given:
        def applicationPath = tempDir.resolve('app')
        writeTestSource(applicationPath, 'src/test/groovy/quizzes/sagas/behaviour/CreateTournamentDynamicEvidenceSmokeTest.groovy', '''
            package quizzes.sagas.behaviour
            class CreateTournamentDynamicEvidenceSmokeTest {}
        ''')
        writeTestSource(applicationPath, 'src/test/groovy/quizzes/sagas/behaviour/DynamicEvidenceDisabledSmokeTest.groovy', '''
            package quizzes.sagas.behaviour
            class DynamicEvidenceDisabledSmokeTest {}
        ''')
        writeTestSource(applicationPath, 'src/test/groovy/quizzes/sagas/behaviour/KeepMeTest.groovy', '''
            package quizzes.sagas.behaviour
            class KeepMeTest {}
        ''')

        when:
        def discovered = new DynamicEnrichmentTestClassDiscoveryService().discover(
                applicationPath,
                dynamicConfig(
                        includeDirs: ['quizzes/sagas'],
                        excludeDirs: [],
                        excludeClasses: [
                                'CreateTournamentDynamicEvidenceSmokeTest',
                                'quizzes.sagas.behaviour.DynamicEvidenceDisabledSmokeTest'
                        ]
                )
        )

        then:
        discovered == ['quizzes.sagas.behaviour.KeepMeTest']
    }

    def 'include and exclude directories reject traversal outside test source root'() {
        given:
        def applicationPath = tempDir.resolve('app')
        writeTestSource(applicationPath, 'src/test/groovy/quizzes/sagas/behaviour/KeepMeTest.groovy', '''
            package quizzes.sagas.behaviour
            class KeepMeTest {}
        ''')

        when:
        new DynamicEnrichmentTestClassDiscoveryService().discover(
                applicationPath,
                dynamicConfig(includeDirs: includeDirs, excludeDirs: excludeDirs, excludeClasses: [])
        )

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message.contains('must stay under test source root')

        where:
        includeDirs | excludeDirs
        ['../other'] | []
        [] | ['../other']
    }

    def 'symlinked files and directories outside the test source root are skipped'() {
        given:
        def applicationPath = tempDir.resolve('app')
        def testRoot = applicationPath.resolve('src/test/groovy')
        writeTestSource(applicationPath, 'src/test/groovy/quizzes/sagas/behaviour/KeepMeTest.groovy', '''
            package quizzes.sagas.behaviour
            class KeepMeTest {}
        ''')

        def outsideFile = tempDir.resolve('outside').resolve('EscapedFileTest.groovy')
        Files.createDirectories(outsideFile.parent)
        Files.writeString(outsideFile, '''
            package escaped.symlink
            class EscapedFileTest {}
        '''.stripIndent().trim() + '\n')
        def symlinkedFile = testRoot.resolve('quizzes/sagas/behaviour/EscapedFileTest.groovy')
        Files.createSymbolicLink(symlinkedFile, outsideFile)

        def outsideDir = tempDir.resolve('outside-dir')
        Files.createDirectories(outsideDir)
        Files.writeString(outsideDir.resolve('NestedEscapedDirTest.groovy'), '''
            package escaped.directory
            class NestedEscapedDirTest {}
        '''.stripIndent().trim() + '\n')
        def symlinkedDir = testRoot.resolve('quizzes/sagas/linked-escape')
        Files.createSymbolicLink(symlinkedDir, outsideDir)

        when:
        def discovered = new DynamicEnrichmentTestClassDiscoveryService().discover(
                applicationPath,
                dynamicConfig(includeDirs: [], excludeDirs: [], excludeClasses: [])
        )

        then:
        discovered == ['quizzes.sagas.behaviour.KeepMeTest']
    }

    def 'package declarations with whitespace around dots are normalized'() {
        given:
        def applicationPath = tempDir.resolve('app')
        writeTestSource(applicationPath, 'src/test/groovy/whitespace/GroovyWhitespaceTest.groovy', '''
            package declared . pkg
            class GroovyWhitespaceTest {}
        ''')
        writeTestSource(applicationPath, 'src/test/groovy/whitespace/JavaWhitespaceTest.java', '''
            package declared . pkg ;
            public class JavaWhitespaceTest {}
        ''')

        when:
        def discovered = new DynamicEnrichmentTestClassDiscoveryService().discover(
                applicationPath,
                dynamicConfig(includeDirs: [], excludeDirs: [], excludeClasses: [])
        )

        then:
        discovered == [
                'declared.pkg.GroovyWhitespaceTest',
                'declared.pkg.JavaWhitespaceTest'
        ]
    }

    def 'derived test class FQN is stable across package declaration and path conventions'() {
        given:
        def applicationPath = tempDir.resolve('app')
        writeTestSource(applicationPath, 'src/test/groovy/path/shape/PackageWinsTest.groovy', '''
            package declared.pkg
            class PackageWinsTest {}
        ''')
        writeTestSource(applicationPath, 'src/test/groovy/path/shape/PathBasedOnlyTest.groovy', '''
            class PathBasedOnlyTest {}
        ''')
        def discovery = new DynamicEnrichmentTestClassDiscoveryService()

        when:
        def first = discovery.discover(applicationPath, dynamicConfig(includeDirs: [], excludeDirs: [], excludeClasses: []))
        def second = discovery.discover(applicationPath, dynamicConfig(includeDirs: [], excludeDirs: [], excludeClasses: []))

        then:
        first == second
        first == [
                'declared.pkg.PackageWinsTest',
                'path.shape.PathBasedOnlyTest'
        ]
    }

    private static DynamicEnrichmentConfig dynamicConfig(Map args) {
        return new DynamicEnrichmentConfig(
                false,
                true,
                'dynamic-evidence',
                'workload-dynamic-evidence.jsonl',
                'workload-dynamic-evidence-manifest.json',
                'dynamic-evidence-join-report.json',
                'src/test/groovy',
                (args.includeDirs as List<String>) ?: [],
                (args.excludeDirs as List<String>) ?: [],
                (args.excludeClasses as List<String>) ?: [],
                300,
                new DynamicEnrichmentConfig.DynamicEnrichmentMavenConfig('mvn', 'test-sagas')
        )
    }

    private static void writeTestSource(Path applicationPath, String relativePath, String source) {
        def file = applicationPath.resolve(relativePath)
        Files.createDirectories(file.parent)
        Files.writeString(file, source.stripIndent().trim() + '\n')
    }
}

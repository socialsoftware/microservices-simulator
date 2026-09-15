package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor

import com.github.javaparser.StaticJavaParser
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.ApplicationsFileTreeParser
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState

class ConstructorCopyVisitorQuizzesSpec extends VisitorTestSupport {
    def setupSpec() {
        configureParser()
    }

    def 'retains the nine qualified Quizzes constructor-copy contracts'() {
        given:
        def root = resolveProjectPath('applications', 'quizzes')
        def parser = new ApplicationsFileTreeParser()
        parser.parse(root)
        def visitor = new ConstructorCopyVisitor()
        def state = new ApplicationAnalysisState()

        when:
        parser.javaFilePaths.values().each { visitor.visit(StaticJavaParser.parse(it.toFile()), state) }
        visitor.finish(state)

        then:
        state.copyContractArtifact.contracts().size() == 9
        state.copyContractArtifact.contracts().every {
            it.fields().containsKey('aggregateId') && it.fields().size() >= 2
        }
    }
}

package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.report

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyConstructorInputTrace
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyFullTraceResult
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyTraceArgument
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyWorkflowCall
import spock.lang.Specification

class AnalysisHtmlReportRendererSpec extends Specification {

    def 'render creates drill-down html sections with unresolved marker breakdown'() {
        given:
        def state = new ApplicationAnalysisState()
        state.groovyConstructorInputTraces.add(new GroovyConstructorInputTrace(
                'com.example.demo.DemoSpec',
                'setup',
                'demoSaga',
                'com.example.demo.CreateOrderFunctionalitySagas'
        ))
        state.groovyFullTraceResults.add(new GroovyFullTraceResult(
                'com.example.demo.DemoSpec',
                'setup',
                'demoSaga',
                'com.example.demo.CreateOrderFunctionalitySagas',
                [
                        new GroovyTraceArgument(0, 'unitOfWorkService [unresolved source-backed variable]'),
                        new GroovyTraceArgument(1, 'createUnitOfWork(...) [unresolved external/runtime edge]')
                ],
                [new GroovyWorkflowCall('demoSaga.executeWorkflow(...)', 'when')],
                ['resolved via helper composeSaga(...) [unresolved helper-cycle]'],
                '''
                demoSaga = new CreateOrderFunctionalitySagas(...)
                arg[0]: unitOfWorkService [unresolved source-backed variable]
                arg[1]: createUnitOfWork(...) [unresolved external/runtime edge]
                '''.stripIndent().trim()
        ))

        def metadata = new AnalysisHtmlReportRenderer.ReportMetadata(
                '/applications',
                'quizzes',
                '2026-04-12T12:00:00Z'
        )

        and:
        def renderer = new AnalysisHtmlReportRenderer()

        when:
        def html = renderer.render(state, metadata, 'Analysis Summary\nunsafe <tag>')

        then:
        html.contains('Verifier Analysis Report')
        html.contains('Groovy Trace Explorer')
        html.contains('Summary to detailed: constructor-input traces (1)')
        html.contains('<th>Binding</th>')
        html.contains('Detailed to deeper: unresolved input markers (3)')
        html.contains('unresolved source-backed variable')
        html.contains('unresolved external/runtime edge')
        html.contains('unresolved helper-cycle')
        html.contains('Full trace details (1)')
        html.contains('args: 2')
        html.contains('Raw Text Report (verbatim)')
        html.contains('unsafe &lt;tag&gt;')
        !html.contains('unsafe <tag>')
    }
}

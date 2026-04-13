package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.report

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyConstructorInputTrace
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyFullTraceResult
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyTraceArgument
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyTraceOriginKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyWorkflowCall
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueRecipe
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
                null,
                GroovyTraceOriginKind.FACADE_CALL,
                'itemFunctionalities.createItem(dto)',
                'com.example.demo.CreateItemFunctionalitySagas',
                [
                        new GroovyTraceArgument(0, 'sagaUnitOfWorkService [unresolved source-backed variable]',
                                new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_VARIABLE, 'sagaUnitOfWorkService', [])),
                        new GroovyTraceArgument(1, 'dto <- new ItemDto()',
                                new GroovyValueRecipe(GroovyValueKind.CONSTRUCTOR, 'ItemDto', [
                                        new GroovyValueRecipe(GroovyValueKind.LITERAL, 'aggregateId', []),
                                        new GroovyValueRecipe(GroovyValueKind.LITERAL, '41', []),
                                        new GroovyValueRecipe(GroovyValueKind.LITERAL, 'orderId', []),
                                        new GroovyValueRecipe(GroovyValueKind.LITERAL, '13', [])
                                ])),
                        new GroovyTraceArgument(2, 'unitOfWork [unresolved runtime edge]',
                                new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_RUNTIME_EDGE, 'createUnitOfWork(...)', [])),
                        new GroovyTraceArgument(3, 'commandGateway [unresolved source-backed variable]',
                                new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_VARIABLE, 'commandGateway', []))
                ],
                [new GroovyWorkflowCall('itemFunctionalities.createItem(...)', 'when')],
                ['resolved via facade ItemFunctionalitiesFacade.createItem(...)'],
                '''
                itemFunctionalities.createItem(dto)
                [binding: (unknown)]
                resolved via facade ItemFunctionalitiesFacade.createItem(...)
                arg[0]: sagaUnitOfWorkService [unresolved source-backed variable]
                arg[1]: dto <- new ItemDto()
                arg[2]: unitOfWork [unresolved runtime edge]
                arg[3]: commandGateway [unresolved source-backed variable]
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
        html.contains('unresolved runtime edge')
        html.contains('resolved via facade ItemFunctionalitiesFacade.createItem(...)')
        html.contains('Full trace details (1)')
        html.contains('[expr: itemFunctionalities.createItem(dto)]')
        html.contains('origin: facade')
        html.contains('Source expression: <code>itemFunctionalities.createItem(dto)</code>')
        html.contains('itemFunctionalities.createItem(dto)')
        html.contains('args: 4')
        html.contains('Raw Text Report (verbatim)')
        html.contains('unsafe &lt;tag&gt;')
        !html.contains('unsafe <tag>')
    }
}

package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.report

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyConstructorInputTrace
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyFullTraceResult
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyRuntimeCallRecipe
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyTraceArgument
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyTraceOriginKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueMetadata
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyWorkflowCall
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceMode
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceModeConfidence
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueResolutionCategory
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueRecipe
import spock.lang.Specification

import java.util.regex.Pattern

class AnalysisHtmlReportRendererSpec extends Specification {
    def 'render shows saga-target-first trace browser with grouped duplicate clusters'() {
        given:
        def state = browserTracesState()

        and:
        def metadata = reportMetadata()
        def renderer = new AnalysisHtmlReportRenderer()

        when:
        def html = renderer.render(state, metadata, 'Analysis Summary')

        then:
        html.contains('data-role="groovy-trace-browser"')
        html.contains('data-default-view="saga"')
        html.contains('By saga target')
        html.contains('By source method')
        html.contains('data-role="trace-expand-all"')
        html.contains('data-role="trace-collapse-all"')

        and:
        (html =~ /data-role="trace-group"[^>]*data-group-kind="saga"[^>]*data-group-label="CreateItemFunctionalitySagas"/).find()
        (html =~ /data-role="trace-group"[^>]*data-group-kind="saga"[^>]*data-group-label="CreateOrderFunctionalitySagas"/).find()
        (html =~ /data-role="trace-cluster"[^>]*data-cluster-source-label="DemoSpec\.setup\(\)"[^>]*data-cluster-saga-label="CreateItemFunctionalitySagas"[^>]*data-instance-count="2"/).find()

        and:
        def traceIds = extractMatches(html, /data-trace-id="(trace-\d+)"/)
        traceIds.size() == 3
        traceIds.unique().size() == 3
        traceIds.every { it ==~ /trace-\d+/ }
        traceIds.every { it.length() <= 10 }
        !html.contains('trace-card-demo-spec-')

        and:
        def browserScript = inlineBrowserScript(html)
        browserScript.contains('data-role=\\"trace-expand-all\\"')
        browserScript.contains('data-role=\\"trace-collapse-all\\"')
        browserScript.contains('setAllDetails(true)')
        browserScript.contains('setAllDetails(false)')
    }

    def 'render exposes source-method alternate view hooks and readable flow summary card'() {
        given:
        def state = browserTracesState()

        and:
        def metadata = reportMetadata()
        def renderer = new AnalysisHtmlReportRenderer()

        when:
        def html = renderer.render(state, metadata, 'Analysis Summary')

        then:
        html.contains('data-role="trace-browser-controls"')
        html.contains('data-role="trace-filter"')
        html.contains('data-role="trace-sort"')
        (html =~ /data-role="trace-group-view"[^>]*data-view="source"/).find()
        (html =~ /<script[^>]*id="groovy-trace-browser-data"[^>]*type="application\/json"/).find()

        and:
        def browserData = scriptContents(html, 'groovy-trace-browser-data')
        browserData.contains('"sourceMethodLabel":"DemoSpec.setup()"')
        browserData.contains('"sourceMethodLabel":"DemoSpec.whenOrderArrives()"')
        browserData.contains('"sagaLabel":"CreateItemFunctionalitySagas"')

        and:
        def setupTraceInstanceSection = firstTraceInstanceSectionByLabels(
                html,
                'DemoSpec.setup()',
                'CreateItemFunctionalitySagas'
        )
        setupTraceInstanceSection.contains('data-role="trace-flow-card"')
        setupTraceInstanceSection.contains('Source method')
        setupTraceInstanceSection.contains('DemoSpec.setup()')
        setupTraceInstanceSection.contains('Source expression')
        setupTraceInstanceSection.contains('itemFunctionalities.createItem(dto)')
        setupTraceInstanceSection.contains('Target saga')
        setupTraceInstanceSection.contains('CreateItemFunctionalitySagas')
        !setupTraceInstanceSection.contains('<svg class="trace-mini-graph"')
    }

    def 'render keeps per-instance raw traces collapsed and preserves escaping in html and embedded data'() {
        given:
        def state = stateWithUnsafeSourceExpression()

        and:
        def metadata = reportMetadata()
        def renderer = new AnalysisHtmlReportRenderer()

        when:
        def html = renderer.render(state, metadata, 'Analysis Summary\nunsafe </script><tag>')

        then:
        def unsafeTraceInstanceSection = firstTraceInstanceSectionByLabels(
                html,
                'DemoSpec.renderUnsafeSection()',
                'RenderFunctionalitySagas'
        )
        (unsafeTraceInstanceSection =~ /<details[^>]*data-role="trace-instance-raw"(?![^>]*\bopen\b)/).find()
        unsafeTraceInstanceSection.contains('htmlFacade.render(&quot;&lt;/script&gt;&lt;tag&gt;&quot;)')
        html.contains('unsafe &lt;/script&gt;&lt;tag&gt;')
        !html.contains('unsafe </script><tag>')

        and:
        def browserData = scriptContents(html, 'groovy-trace-browser-data')
        browserData.contains('\\u003c/script\\u003e\\u003ctag\\u003e')
        !browserData.contains('</script>')
        !browserData.contains('<tag>')
    }

    def 'report renders injectable placeholder as replay classification'() {
        given:
        def state = replayClassificationState()

        and:
        def metadata = reportMetadata()
        def renderer = new AnalysisHtmlReportRenderer()

        when:
        def html = renderer.render(state, metadata, 'Analysis Summary')

        then:
        def injectableTraceSection = firstTraceInstanceSectionByLabels(
                html,
                'DemoSpec.replayInjectable()',
                'CreateItemFunctionalitySagas'
        )
        injectableTraceSection.contains('replay: injectable placeholder')
        injectableTraceSection.contains('type: com.example.demo.SagaUnitOfWorkService')
        injectableTraceSection.contains('sagaUnitOfWorkService [unresolved source-backed variable]')
        injectableTraceSection.contains('UNRESOLVED_VARIABLE: sagaUnitOfWorkService')
    }

    def 'report renders runtime call metadata without hiding unresolved marker'() {
        given:
        def state = replayClassificationState()

        and:
        def metadata = reportMetadata()
        def renderer = new AnalysisHtmlReportRenderer()

        when:
        def html = renderer.render(state, metadata, 'Analysis Summary')

        then:
        def runtimeTraceSection = firstTraceInstanceSectionByLabels(
                html,
                'DemoSpec.replayRuntimeCall()',
                'CreateOrderFunctionalitySagas'
        )
        runtimeTraceSection.contains('replay: runtime call')
        runtimeTraceSection.contains('call: runtimeGateway.loadExternalDto()')
        runtimeTraceSection.contains('type: com.example.demo.ExternalDto')
        runtimeTraceSection.contains('[unresolved external/runtime edge]')
        runtimeTraceSection.contains('UNRESOLVED_RUNTIME_EDGE: runtimeGateway.loadExternalDto()')
    }

    def 'report exposes source-mode summary and per-trace evidence badges'() {
        given:
        def state = sourceModeState()

        and:
        def metadata = reportMetadata()
        def renderer = new AnalysisHtmlReportRenderer()

        when:
        def html = renderer.render(state, metadata, 'Analysis Summary')

        then:
        def sourceModeSummary = sourceModeSummarySection(html)
        sourceModeSummary.contains('<span class="chip source-mode-sagas">SAGAS</span>')
        sourceModeSummary.contains('<span class="chip source-mode-tcc">TCC</span>')
        sourceModeSummary.contains('<td>1</td>')
        sourceModeSummary.contains('LocalBeanConfiguration -&gt; @Bean unitOfWorkService returns SagaUnitOfWorkService')
        sourceModeSummary.contains('LocalBeanConfigurationCausal -&gt; @Bean unitOfWorkService returns CausalUnitOfWorkService')

        and:
        def sagaTraceSection = firstTraceInstanceSectionByLabels(
                html,
                'SagaSpec.createSagaOrder()',
                'CreateOrderFunctionalitySagas'
        )
        sagaTraceSection.contains('source mode: SAGAS')
        sagaTraceSection.contains('confidence: TEST_CONFIGURATION')
        sagaTraceSection.contains('LocalBeanConfiguration -&gt; @Bean unitOfWorkService returns SagaUnitOfWorkService')

        and:
        def tccTraceSection = firstTraceInstanceSectionByLabels(
                html,
                'CausalSpec.createCausalOrder()',
                'CreateOrderFunctionalitySagas'
        )
        tccTraceSection.contains('source mode: TCC')
        tccTraceSection.contains('confidence: TEST_CONFIGURATION')
        tccTraceSection.contains('LocalBeanConfigurationCausal -&gt; @Bean unitOfWorkService returns CausalUnitOfWorkService')
    }

    def 'report summary separates injectable placeholders from runtime edges'() {
        given:
        def state = replayClassificationState()

        and:
        def metadata = reportMetadata()
        def renderer = new AnalysisHtmlReportRenderer()

        when:
        def html = renderer.render(state, metadata, 'Analysis Summary')

        then:
        def replaySummary = replaySummarySection(html)
        (replaySummary =~ /<tr><td><span class="chip severity-high">replay: runtime call<\/span><\/td><td><span class="chip severity-high">high<\/span><\/td><td>1<\/td><\/tr>/).find()
        (replaySummary =~ /<tr><td><span class="chip severity-low">replay: injectable placeholder<\/span><\/td><td><span class="chip severity-low">low<\/span><\/td><td>1<\/td><\/tr>/).find()
        replaySummary.indexOf('replay: runtime call') < replaySummary.indexOf('replay: injectable placeholder')
    }

    private static List<String> extractMatches(String html, String pattern) {
        def matcher = html =~ pattern
        def matches = []
        while (matcher.find()) {
            matches << matcher.group(1)
        }
        matches
    }

    private static String scriptContents(String html, String scriptId) {
        def matcher = (html =~ /(?s)<script[^>]*id="${Pattern.quote(scriptId)}"[^>]*>(.*?)<\/script>/)
        assert matcher.find(): "Missing script tag '${scriptId}'"
        matcher.group(1)
    }

    private static String inlineBrowserScript(String html) {
        def matcher = (html =~ /(?s)<script>(.*?)<\/script>/)
        assert matcher.find(): 'Missing inline browser script'
        matcher.group(1)
    }

    private static String replaySummarySection(String html) {
        def matcher = (html =~ /(?s)<details class="subsection"><summary>Replay-oriented unresolved categories \(\d+\)<\/summary><div class="subsection-content">(.*?)<\/div><\/details>/)
        assert matcher.find(): 'Missing replay summary section'
        matcher.group(1)
    }

    private static String sourceModeSummarySection(String html) {
        def matcher = (html =~ /(?s)<details class="subsection"><summary>Source-mode classification \(\d+ trace\(s\)\)<\/summary><div class="subsection-content">(.*?)<\/div><\/details>/)
        assert matcher.find(): 'Missing source-mode summary section'
        matcher.group(1)
    }

    private static String firstTraceInstanceSectionByLabels(String html,
                                                            String sourceMethodLabel,
                                                            String sagaLabel) {
        def matcher = (html =~ /(?s)<article[^>]*data-role="trace-instance"[^>]*>.*?${Pattern.quote(sourceMethodLabel)}.*?${Pattern.quote(sagaLabel)}.*?<\/article>/)
        assert matcher.find(): "Missing trace instance for ${sourceMethodLabel} -> ${sagaLabel}"
        matcher.group(0)
    }

    private static AnalysisHtmlReportRenderer.ReportMetadata reportMetadata() {
        new AnalysisHtmlReportRenderer.ReportMetadata(
                '/applications',
                'quizzes',
                '2026-04-12T12:00:00Z'
        )
    }

    private static ApplicationAnalysisState browserTracesState() {
        def state = new ApplicationAnalysisState()
        state.groovyConstructorInputTraces.add(new GroovyConstructorInputTrace(
                'com.example.demo.DemoSpec',
                'setup',
                'itemSaga',
                'com.example.demo.CreateItemFunctionalitySagas'
        ))
        state.groovyConstructorInputTraces.add(new GroovyConstructorInputTrace(
                'com.example.demo.DemoSpec',
                'setup',
                'itemSagaRetry',
                'com.example.demo.CreateItemFunctionalitySagas'
        ))
        state.groovyConstructorInputTraces.add(new GroovyConstructorInputTrace(
                'com.example.demo.DemoSpec',
                'whenOrderArrives',
                'orderSaga',
                'com.example.demo.CreateOrderFunctionalitySagas'
        ))

        state.groovyFullTraceResults.add(new GroovyFullTraceResult(
                'com.example.demo.DemoSpec',
                'setup',
                'itemSaga',
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
                                new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_RUNTIME_EDGE, 'createUnitOfWork(...)', []))
                ],
                [new GroovyWorkflowCall('itemFunctionalities.createItem(...)', 'when')],
                ['resolved via facade ItemFunctionalitiesFacade.createItem(...)'],
                '''
                itemFunctionalities.createItem(dto)
                [binding: itemSaga]
                resolved via facade ItemFunctionalitiesFacade.createItem(...)
                arg[0]: sagaUnitOfWorkService [unresolved source-backed variable]
                arg[1]: dto <- new ItemDto()
                arg[2]: unitOfWork [unresolved runtime edge]
                '''.stripIndent().trim()
        ))

        state.groovyFullTraceResults.add(new GroovyFullTraceResult(
                'com.example.demo.DemoSpec',
                'setup',
                'itemSagaRetry',
                GroovyTraceOriginKind.FACADE_CALL,
                'itemFunctionalities.createItem(dtoRetry)',
                'com.example.demo.CreateItemFunctionalitySagas',
                [
                        new GroovyTraceArgument(0, 'sagaUnitOfWorkService [unresolved source-backed variable]',
                                new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_VARIABLE, 'sagaUnitOfWorkService', [])),
                        new GroovyTraceArgument(1, 'dtoRetry <- new ItemDto()',
                                new GroovyValueRecipe(GroovyValueKind.CONSTRUCTOR, 'ItemDto', [
                                        new GroovyValueRecipe(GroovyValueKind.LITERAL, 'aggregateId', []),
                                        new GroovyValueRecipe(GroovyValueKind.LITERAL, '42', []),
                                        new GroovyValueRecipe(GroovyValueKind.LITERAL, 'orderId', []),
                                        new GroovyValueRecipe(GroovyValueKind.LITERAL, '14', [])
                                ])),
                        new GroovyTraceArgument(2, 'unitOfWork [unresolved runtime edge]',
                                new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_RUNTIME_EDGE, 'retryUnitOfWork(...)', []))
                ],
                [new GroovyWorkflowCall('itemFunctionalities.createItem(...)', 'retry-when')],
                ['resolved via facade ItemFunctionalitiesFacade.createItem(...)'],
                '''
                itemFunctionalities.createItem(dtoRetry)
                [binding: itemSagaRetry]
                resolved via facade ItemFunctionalitiesFacade.createItem(...)
                arg[0]: sagaUnitOfWorkService [unresolved source-backed variable]
                arg[1]: dtoRetry <- new ItemDto()
                arg[2]: unitOfWork [unresolved runtime edge]
                '''.stripIndent().trim()
        ))

        state.groovyFullTraceResults.add(new GroovyFullTraceResult(
                'com.example.demo.DemoSpec',
                'whenOrderArrives',
                'orderSaga',
                GroovyTraceOriginKind.DIRECT_CONSTRUCTOR,
                'new CreateOrderFunctionalitySagas(orderDto)',
                'com.example.demo.CreateOrderFunctionalitySagas',
                [
                        new GroovyTraceArgument(0, 'orderDto <- new OrderDto()',
                                new GroovyValueRecipe(GroovyValueKind.CONSTRUCTOR, 'OrderDto', [
                                        new GroovyValueRecipe(GroovyValueKind.LITERAL, 'orderId', []),
                                        new GroovyValueRecipe(GroovyValueKind.LITERAL, '99', [])
                                ])),
                        new GroovyTraceArgument(1, 'cycleGuard [unresolved cyclic reference]',
                                new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_VARIABLE, 'cycleGuard', []))
                ],
                [new GroovyWorkflowCall('new CreateOrderFunctionalitySagas(...)', 'then')],
                ['fallback constructor path [unresolved cyclic reference]'],
                '''
                new CreateOrderFunctionalitySagas(orderDto)
                [binding: orderSaga]
                arg[0]: orderDto <- new OrderDto()
                arg[1]: cycleGuard [unresolved cyclic reference]
                '''.stripIndent().trim()
        ))

        state
    }

    private static ApplicationAnalysisState stateWithUnsafeSourceExpression() {
        def state = new ApplicationAnalysisState()
        state.groovyConstructorInputTraces.add(new GroovyConstructorInputTrace(
                'com.example.demo.DemoSpec',
                'renderUnsafeSection',
                null,
                'com.example.demo.RenderFunctionalitySagas'
        ))
        state.groovyFullTraceResults.add(new GroovyFullTraceResult(
                'com.example.demo.DemoSpec',
                'renderUnsafeSection',
                null,
                GroovyTraceOriginKind.FACADE_CALL,
                'htmlFacade.render("</script><tag>")',
                'com.example.demo.RenderFunctionalitySagas',
                [new GroovyTraceArgument(0, 'payload <- "</script><tag>"',
                        new GroovyValueRecipe(GroovyValueKind.LITERAL, '</script><tag>', []))],
                [new GroovyWorkflowCall('htmlFacade.render(...)', 'when')],
                ['resolved via html facade RenderFacade.render(...)'],
                '''
                htmlFacade.render("</script><tag>")
                arg[0]: payload <- "</script><tag>"
                '''.stripIndent().trim()
        ))
        state
    }

    private static ApplicationAnalysisState sourceModeState() {
        def state = new ApplicationAnalysisState()
        state.groovyFullTraceResults.add(new GroovyFullTraceResult(
                'com.example.demo.SagaSpec',
                'createSagaOrder',
                null,
                GroovyTraceOriginKind.FACADE_CALL,
                'orderFunctionalities.createOrder(dto)',
                'com.example.demo.CreateOrderFunctionalitySagas',
                SourceMode.SAGAS,
                SourceModeConfidence.TEST_CONFIGURATION,
                ['LocalBeanConfiguration -> @Bean unitOfWorkService returns SagaUnitOfWorkService'],
                [new GroovyTraceArgument(0, 'dto <- new OrderDto()',
                        new GroovyValueRecipe(GroovyValueKind.CONSTRUCTOR, 'OrderDto', []))],
                [new GroovyWorkflowCall('orderFunctionalities.createOrder(...)', 'when')],
                ['resolved via facade OrderFunctionalities.createOrder(...)'],
                '''
                orderFunctionalities.createOrder(dto)
                arg[0]: dto <- new OrderDto()
                '''.stripIndent().trim()
        ))
        state.groovyFullTraceResults.add(new GroovyFullTraceResult(
                'com.example.demo.CausalSpec',
                'createCausalOrder',
                null,
                GroovyTraceOriginKind.FACADE_CALL,
                'orderFunctionalities.createOrder(dto)',
                'com.example.demo.CreateOrderFunctionalitySagas',
                SourceMode.TCC,
                SourceModeConfidence.TEST_CONFIGURATION,
                ['LocalBeanConfigurationCausal -> @Bean unitOfWorkService returns CausalUnitOfWorkService'],
                [new GroovyTraceArgument(0, 'dto <- new OrderDto()',
                        new GroovyValueRecipe(GroovyValueKind.CONSTRUCTOR, 'OrderDto', []))],
                [new GroovyWorkflowCall('orderFunctionalities.createOrder(...)', 'when')],
                ['resolved via facade OrderFunctionalities.createOrder(...)'],
                '''
                orderFunctionalities.createOrder(dto)
                arg[0]: dto <- new OrderDto()
                '''.stripIndent().trim()
        ))
        state
    }

    private static ApplicationAnalysisState replayClassificationState() {
        def state = new ApplicationAnalysisState()
        state.groovyConstructorInputTraces.add(new GroovyConstructorInputTrace(
                'com.example.demo.DemoSpec',
                'replayInjectable',
                'injectablePlaceholder',
                'com.example.demo.CreateItemFunctionalitySagas'
        ))
        state.groovyConstructorInputTraces.add(new GroovyConstructorInputTrace(
                'com.example.demo.DemoSpec',
                'replayRuntimeCall',
                'runtimeCall',
                'com.example.demo.CreateOrderFunctionalitySagas'
        ))

        state.groovyFullTraceResults.add(new GroovyFullTraceResult(
                'com.example.demo.DemoSpec',
                'replayInjectable',
                'injectablePlaceholder',
                GroovyTraceOriginKind.FACADE_CALL,
                'itemFunctionalities.createItem(dto)',
                'com.example.demo.CreateItemFunctionalitySagas',
                [new GroovyTraceArgument(
                        0,
                        'sagaUnitOfWorkService [unresolved source-backed variable]',
                        new GroovyValueRecipe(
                                GroovyValueKind.UNRESOLVED_VARIABLE,
                                'sagaUnitOfWorkService',
                                [],
                                new GroovyValueMetadata(
                                        GroovyValueResolutionCategory.INJECTABLE_PLACEHOLDER,
                                        'com.example.demo.SagaUnitOfWorkService',
                                        'injectable-placeholder:demo:saga-unit-of-work-service',
                                        null
                                )
                        ),
                        'com.example.demo.SagaUnitOfWorkService'
                )],
                [new GroovyWorkflowCall('itemFunctionalities.createItem(...)', 'when')],
                ['resolved via facade ItemFunctionalitiesFacade.createItem(...)'],
                '''
                itemFunctionalities.createItem(dto)
                [binding: injectablePlaceholder]
                arg[0]: sagaUnitOfWorkService [unresolved source-backed variable]
                '''.stripIndent().trim()
        ))

        state.groovyFullTraceResults.add(new GroovyFullTraceResult(
                'com.example.demo.DemoSpec',
                'replayRuntimeCall',
                'runtimeCall',
                GroovyTraceOriginKind.FACADE_CALL,
                'runtimeGateway.loadExternalDto()',
                'com.example.demo.CreateOrderFunctionalitySagas',
                [new GroovyTraceArgument(
                        0,
                        'runtimeDto <- runtimeGateway.loadExternalDto() [unresolved external/runtime edge]',
                        new GroovyValueRecipe(
                                GroovyValueKind.UNRESOLVED_RUNTIME_EDGE,
                                'runtimeGateway.loadExternalDto()',
                                [],
                                new GroovyValueMetadata(
                                        GroovyValueResolutionCategory.RUNTIME_CALL,
                                        'com.example.demo.ExternalDto',
                                        null,
                                        new GroovyRuntimeCallRecipe(
                                                'runtimeGateway',
                                                'loadExternalDto',
                                                [],
                                                'runtimeGateway.loadExternalDto()'
                                        )
                                )
                        ),
                        'com.example.demo.ExternalDto'
                )],
                [new GroovyWorkflowCall('runtimeGateway.loadExternalDto(...)', 'when')],
                ['resolved via runtime gateway'],
                '''
                runtimeGateway.loadExternalDto()
                [binding: runtimeCall]
                arg[0]: runtimeDto <- runtimeGateway.loadExternalDto() [unresolved external/runtime edge]
                '''.stripIndent().trim()
        ))

        state
    }
}

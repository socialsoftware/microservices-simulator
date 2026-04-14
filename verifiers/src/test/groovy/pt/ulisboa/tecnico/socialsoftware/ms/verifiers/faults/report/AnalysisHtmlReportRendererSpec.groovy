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

import java.util.regex.Pattern

class AnalysisHtmlReportRendererSpec extends Specification {
    private static final String ITEM_TRACE_CARD_ID = 'trace-card-demo-spec-setup-create-item-functionality-sagas'
    private static final String ORDER_TRACE_CARD_ID = 'trace-card-demo-spec-when-order-arrives-create-order-functionality-sagas'
    private static final String UNSAFE_TRACE_CARD_ID = 'trace-card-demo-spec-render-unsafe-section-render-functionality-sagas'
    private static final String UNSAFE_MINI_GRAPH_ID = 'trace-mini-graph-demo-spec-render-unsafe-section-render-functionality-sagas'

    def 'render includes grouped views by source method, saga target, and unresolved marker category'() {
        given:
        def state = groupedTracesState()

        and:
        def metadata = reportMetadata()
        def renderer = new AnalysisHtmlReportRenderer()

        when:
        def html = renderer.render(state, metadata, 'Analysis Summary')

        then:
        def sourceSection = sectionBetweenMarkers(html, 'Grouped by source method', 'Grouped by saga target')
        def sagaSection = sectionBetweenMarkers(html, 'Grouped by saga target', 'Grouped by unresolved marker category')
        def unresolvedSection = sectionBetweenMarkers(html, 'Grouped by unresolved marker category', 'Sagas and Steps (')

        assertGroupedEntry(sourceSection, 'DemoSpec.setup()', ITEM_TRACE_CARD_ID)
        assertGroupedEntry(sourceSection, 'DemoSpec.whenOrderArrives()', ORDER_TRACE_CARD_ID)

        assertGroupedEntry(sagaSection, 'CreateItemFunctionalitySagas', ITEM_TRACE_CARD_ID)
        assertGroupedEntry(sagaSection, 'CreateOrderFunctionalitySagas', ORDER_TRACE_CARD_ID)

        assertGroupedEntry(unresolvedSection, 'unresolved runtime edge', ITEM_TRACE_CARD_ID)
        assertGroupedEntry(unresolvedSection, 'unresolved source-backed variable', ITEM_TRACE_CARD_ID)
        assertGroupedEntry(unresolvedSection, 'unresolved cyclic reference', ORDER_TRACE_CARD_ID)
    }

    def 'render includes structured trace card with severity chips and arguments table'() {
        given:
        def state = groupedTracesState()

        and:
        def metadata = reportMetadata()
        def renderer = new AnalysisHtmlReportRenderer()

        when:
        def html = renderer.render(state, metadata, 'Analysis Summary')

        then:
        def itemTraceCardSection = traceCardSection(html, ITEM_TRACE_CARD_ID)

        itemTraceCardSection.contains('Severity: runtime edge (high)')
        itemTraceCardSection.contains('Severity: source-backed variable (medium)')
        (itemTraceCardSection =~ /(?s)<table[^>]*data-role="trace-arguments"[^>]*>.*?<th>Arg<\/th>.*?<th>Provenance<\/th>.*?<th>Value recipe<\/th>/).find()
        (itemTraceCardSection =~ /(?s)<table[^>]*data-role="trace-arguments"[^>]*>.*?<tr[^>]*>.*?<td[^>]*>(?:arg\[0\]|0)<\/td>.*?<td[^>]*>sagaUnitOfWorkService \[unresolved source-backed variable\]<\/td>.*?<td[^>]*>.*?sagaUnitOfWorkService.*?<\/td>/).find()
        (itemTraceCardSection =~ /(?s)data-role="workflow-breadcrumbs".*?itemFunctionalities\.createItem\(\.\.\.\).*?when/).find()
        (itemTraceCardSection =~ /(?s)data-role="resolution-notes".*?resolved via facade ItemFunctionalitiesFacade\.createItem\(\.\.\.\)/).find()
    }

    def 'render includes static mini graph svg and preserves raw text escaping'() {
        given:
        def state = stateWithUnsafeSourceExpression()

        and:
        def metadata = reportMetadata()
        def renderer = new AnalysisHtmlReportRenderer()

        when:
        def html = renderer.render(state, metadata, 'Analysis Summary\nunsafe <tag>')

        then:
        def sourceSection = sectionBetweenMarkers(html, 'Grouped by source method', 'Grouped by saga target')
        def unsafeCardSection = traceCardSection(html, UNSAFE_TRACE_CARD_ID)

        sourceSection.contains("href=\"#${UNSAFE_TRACE_CARD_ID}\"")
        unsafeCardSection.contains("id=\"${UNSAFE_TRACE_CARD_ID}\"")
        (unsafeCardSection =~ /(?s)<svg[^>]*id="${Pattern.quote(UNSAFE_MINI_GRAPH_ID)}"[^>]*data-trace-card-ref="${Pattern.quote(UNSAFE_TRACE_CARD_ID)}"/).find()
        html.contains('Source expression: <code>htmlFacade.render(&quot;&lt;tag&gt;&quot;)</code>')
        html.contains('unsafe &lt;tag&gt;')
        !html.contains('unsafe <tag>')
    }

    private static void assertGroupedEntry(String sectionHtml, String groupedKey, String traceCardId) {
        assert sectionHtml.contains(groupedKey)
        assert (sectionHtml =~ /(?s)${Pattern.quote(groupedKey)}.*?${countMarkerRegex()}/).find()
        assert (sectionHtml =~ /(?s)${Pattern.quote(groupedKey)}.*?href="#${Pattern.quote(traceCardId)}"/).find()
    }

    private static String sectionBetweenMarkers(String html, String startMarker, String endMarker) {
        int start = html.indexOf(startMarker)
        assert start >= 0: "Missing section marker '${startMarker}'"
        int end = html.indexOf(endMarker, start + startMarker.length())
        assert end >= 0: "Missing section marker '${endMarker}' after '${startMarker}'"
        html.substring(start, end)
    }

    private static String traceCardSection(String html, String traceCardId) {
        String idMarker = "id=\"${traceCardId}\""
        int start = html.indexOf(idMarker)
        assert start >= 0: "Missing trace card id '${traceCardId}'"

        int nextCardStart = html.indexOf('id="trace-card-', start + idMarker.length())
        int end = nextCardStart >= 0 ? nextCardStart : html.length()
        html.substring(start, end)
    }

    private static String countMarkerRegex() {
        '(?:\\b1\\b\\s*(?:trace|traces|marker|markers)|trace\\(s\\):\\s*1|marker\\(s\\):\\s*1|x1|count\\s*[:=]\\s*1)'
    }

    private static AnalysisHtmlReportRenderer.ReportMetadata reportMetadata() {
        new AnalysisHtmlReportRenderer.ReportMetadata(
                '/applications',
                'quizzes',
                '2026-04-12T12:00:00Z'
        )
    }

    private static ApplicationAnalysisState groupedTracesState() {
        def state = new ApplicationAnalysisState()
        state.groovyConstructorInputTraces.add(new GroovyConstructorInputTrace(
                'com.example.demo.DemoSpec',
                'setup',
                'itemSaga',
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
                                new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_RUNTIME_EDGE, 'createUnitOfWork(...)', []))
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
                'htmlFacade.render("<tag>")',
                'com.example.demo.RenderFunctionalitySagas',
                [new GroovyTraceArgument(0, 'payload <- "<tag>"',
                        new GroovyValueRecipe(GroovyValueKind.LITERAL, '<tag>', []))],
                [new GroovyWorkflowCall('htmlFacade.render(...)', 'when')],
                ['resolved via html facade RenderFacade.render(...)'],
                '''
                htmlFacade.render("<tag>")
                arg[0]: payload <- "<tag>"
                '''.stripIndent().trim()
        ))
        state
    }
}

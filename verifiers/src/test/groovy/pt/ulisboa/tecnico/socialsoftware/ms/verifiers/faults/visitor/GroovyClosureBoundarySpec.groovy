package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovySourceIndex
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

/** Characterizes the current boundary; does not promise closure execution support. */
class GroovyClosureBoundarySpec extends VisitorTestSupport {
    @TempDir Path sources

    def 'direct facade tracing is independent of benchmark names and does not enter closures'() {
        given:
        configureParser()
        def state = new ApplicationAnalysisState()
        def units = parseAllDummyappFiles()
        def workflow = new WorkflowFunctionalityVisitor()
        def creation = new WorkflowFunctionalityCreationSiteVisitor()
        units.each { workflow.visit(it, state) }
        units.each { creation.visit(it, state) }
        Files.writeString(sources.resolve('ClosureBoundaryFixture.groovy'), '''
            package com.example.dummyapp
            import com.example.dummyapp.item.aggregate.ItemDto
            import com.example.dummyapp.item.coordination.ItemFunctionalitiesFacade
            import spock.lang.Specification

            class ClosureBoundaryFixture extends Specification {
                ItemFunctionalitiesFacade itemFunctionalities

                def 'benchmark direct call'() {
                    itemFunctionalities.createItem(new ItemDto(aggregateId: 41, orderId: 13))
                }

                def 'ordinary direct call'() {
                    itemFunctionalities.createItem(new ItemDto(aggregateId: 41, orderId: 13))
                }

                def 'ordinary each call'() {
                    (1..3).each { idx ->
                        itemFunctionalities.createItem(new ItemDto(aggregateId: 41, orderId: 13))
                    }
                }

                def 'ordinary unknown closure call'() {
                    externalRunner.run {
                        itemFunctionalities.createItem(new ItemDto(aggregateId: 41, orderId: 13))
                    }
                }
            }
        ''')
        def index = new GroovySourceIndex()
        index.parse(sources)

        when:
        new GroovyConstructorInputTraceVisitor().visit(index, state)

        then: 'the source index includes every method, including closure-bearing methods'
        index.classesByFqn['com.example.dummyapp.ClosureBoundaryFixture'].methods()*.name()
                .containsAll(['benchmark direct call', 'ordinary direct call',
                              'ordinary each call', 'ordinary unknown closure call'])

        and: 'only the two direct calls reach state, even with identical literal arguments'
        def traces = state.groovyFullTraceResults.findAll {
            it.sourceClassFqn() == 'com.example.dummyapp.ClosureBoundaryFixture'
        }
        traces*.sourceMethodName().sort() == ['benchmark direct call', 'ordinary direct call']
        traces.every {
            it.sagaClassFqn() == 'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas'
        }
    }
}

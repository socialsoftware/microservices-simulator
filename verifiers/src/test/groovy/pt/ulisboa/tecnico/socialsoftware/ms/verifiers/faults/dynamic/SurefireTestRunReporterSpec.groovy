package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

class SurefireTestRunReporterSpec extends Specification {
    private final ObjectMapper mapper = new ObjectMapper()

    @TempDir
    Path tempDir

    def 'maps representative surefire reports to per-class statuses and deterministic counts'() {
        given:
        def reportsDir = tempDir.resolve('surefire-reports')
        def evidenceDir = tempDir.resolve('dynamic-evidence')
        Files.createDirectories(reportsDir)
        report(reportsDir, 'com.example.PassedSpec', 2, 0, 0, 0)
        report(reportsDir, 'com.example.FailedSpec', 2, 1, 0, 0)
        report(reportsDir, 'com.example.ErrorSpec', 2, 0, 1, 0)
        report(reportsDir, 'com.example.ZeroTestSpec', 0, 0, 0, 0)
        report(reportsDir, 'com.example.SkippedSpec', 3, 0, 0, 3)

        when:
        def result = new SurefireTestRunReporter(mapper).write(reportsDir, evidenceDir, [
                'com.example.ZeroTestSpec',
                'com.example.SkippedSpec',
                'com.example.PassedSpec',
                'com.example.FailedSpec',
                'com.example.ErrorSpec'
        ], false)

        then:
        result.records()*.testClassFqn() == [
                'com.example.ErrorSpec',
                'com.example.FailedSpec',
                'com.example.PassedSpec',
                'com.example.SkippedSpec',
                'com.example.ZeroTestSpec'
        ]
        result.records().find { it.testClassFqn() == 'com.example.PassedSpec' }.status() == 'PASSED'
        result.records().find { it.testClassFqn() == 'com.example.FailedSpec' }.status() == 'FAILED'
        result.records().find { it.testClassFqn() == 'com.example.ErrorSpec' }.status() == 'FAILED'
        result.records().find { it.testClassFqn() == 'com.example.ZeroTestSpec' }.status() == 'PASSED'
        result.records().find { it.testClassFqn() == 'com.example.SkippedSpec' }.status() == 'SKIPPED'
        result.statusCounts() == [passed: 2, failed: 2, timedOut: 0, skipped: 1, noReport: 0]
        result.warnings().isEmpty()

        and:
        def batch = mapper.readTree(Files.readString(evidenceDir.resolve('test-run.json')))
        batch.path('statusCounts').path('passed').asInt() == 2
        batch.path('statusCounts').path('failed').asInt() == 2
        batch.path('statusCounts').path('timedOut').asInt() == 0
        batch.path('statusCounts').path('skipped').asInt() == 1
        batch.path('statusCounts').path('noReport').asInt() == 0
        mapper.readTree(Files.readString(evidenceDir.resolve('test-runs/com.example.PassedSpec.json'))).path('status').asText() == 'PASSED'
        mapper.readTree(Files.readString(evidenceDir.resolve('test-runs/com.example.SkippedSpec.json'))).path('status').asText() == 'SKIPPED'
    }

    def 'marks missing selected reports as no report when maven completed'() {
        given:
        def reportsDir = tempDir.resolve('surefire-reports')
        def evidenceDir = tempDir.resolve('dynamic-evidence')
        Files.createDirectories(reportsDir)

        when:
        def result = new SurefireTestRunReporter(mapper).write(reportsDir, evidenceDir, ['com.example.MissingSpec'], false)

        then:
        result.records()[0].status() == 'NO_REPORT'
        result.statusCounts() == [passed: 0, failed: 0, timedOut: 0, skipped: 0, noReport: 1]
        result.warnings()[0].contains('No Surefire report found for selected test class com.example.MissingSpec')
        def sidecar = mapper.readTree(Files.readString(evidenceDir.resolve('test-runs/com.example.MissingSpec.json')))
        sidecar.path('status').asText() == 'NO_REPORT'
        sidecar.path('warning').asText().contains('No Surefire report found')
    }

    def 'marks missing selected non-runnable source class as passed when maven completed'() {
        given:
        def appDir = tempDir.resolve('app')
        def reportsDir = appDir.resolve('target/surefire-reports')
        def sourceDir = appDir.resolve('src/test/groovy/com/example')
        def evidenceDir = tempDir.resolve('dynamic-evidence')
        Files.createDirectories(reportsDir)
        Files.createDirectories(sourceDir)
        Files.writeString(sourceDir.resolve('MissingAsyncSpec.groovy'), '''
                package com.example

                class MissingAsyncSpec /*extends BaseSpec*/ {
                    def setup() {}
                    def 'feature-shaped method but non-runnable class'() {}
                }
                ''')

        when:
        def result = new SurefireTestRunReporter(mapper).write(reportsDir, appDir, evidenceDir, ['com.example.MissingAsyncSpec'], false)

        then:
        result.records()[0].status() == 'PASSED'
        result.records()[0].tests() == 0
        result.statusCounts() == [passed: 1, failed: 0, timedOut: 0, skipped: 0, noReport: 0]
        result.warnings().isEmpty()
    }

    def 'marks missing selected reports as timed out when maven timed out'() {
        given:
        def evidenceDir = tempDir.resolve('dynamic-evidence')

        when:
        def result = new SurefireTestRunReporter(mapper).write(tempDir.resolve('missing-reports'), evidenceDir, ['com.example.TimeoutSpec'], true)

        then:
        result.records()[0].status() == 'TIMED_OUT'
        result.statusCounts() == [passed: 0, failed: 0, timedOut: 1, skipped: 0, noReport: 0]
        result.warnings().isEmpty()
        mapper.readTree(Files.readString(evidenceDir.resolve('test-runs/com.example.TimeoutSpec.json'))).path('status').asText() == 'TIMED_OUT'
    }

    private static void report(Path reportsDir, String testClassFqn, int tests, int failures, int errors, int skipped) {
        Files.writeString(reportsDir.resolve("TEST-${testClassFqn}.xml"), """
                <testsuite name="${testClassFqn}" classname="${testClassFqn}" tests="${tests}" failures="${failures}" errors="${errors}" skipped="${skipped}">
                </testsuite>
                """)
    }
}

package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.report;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.CommandDispatchInfo;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.CommandHandlerBuildingBlock;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.DispatchMultiplicity;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.SagaFunctionalityBuildingBlock;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.SagaStepBuildingBlock;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.ServiceBuildingBlock;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.StepDispatchFootprint;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.WorkflowFunctionalityCreationSite;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyConstructorInputTrace;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyFullTraceResult;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AnalysisHtmlReportRenderer {
    private static final Pattern UNRESOLVED_NOTE_PATTERN = Pattern.compile("\\[(unresolved [^\\]]+)\\]");
    private static final Pattern ARG_INDEX_PATTERN = Pattern.compile("^\\s*arg\\[(\\d+)]");

    public String render(ApplicationAnalysisState state,
                         ReportMetadata metadata,
                         String rawTextReport) {
        StringBuilder html = new StringBuilder(32_000);

        List<GroovyConstructorInputTrace> constructorTraces = state.groovyConstructorInputTraces.stream()
                .sorted(Comparator.comparing(GroovyConstructorInputTrace::sourceClassFqn)
                        .thenComparing(GroovyConstructorInputTrace::sourceMethodName)
                        .thenComparing(GroovyConstructorInputTrace::sagaClassFqn))
                .toList();

        List<GroovyTraceView> fullTraceViews = state.groovyFullTraceResults.stream()
                .sorted(Comparator.comparing(GroovyFullTraceResult::sourceClassFqn)
                        .thenComparing(GroovyFullTraceResult::sourceMethodName)
                        .thenComparing(GroovyFullTraceResult::sagaClassFqn))
                .map(this::toGroovyTraceView)
                .toList();

        Map<String, Integer> unresolvedByCategory = collectUnresolvedByCategory(fullTraceViews);

        appendDocumentStart(html, metadata);
        appendSummaryCards(html, state, unresolvedByCategory);

        appendGroovySection(html, constructorTraces, fullTraceViews, unresolvedByCategory);
        appendSagasSection(html, state);
        appendCommandHandlersSection(html, state);
        appendServicesSection(html, state);
        appendDispatchTargetsSection(html, state);
        appendCreationSitesSection(html, state);
        appendInterfaceIndexSection(html, state);
        appendRawTextSection(html, rawTextReport);

        appendDocumentEnd(html);
        return html.toString();
    }

    private void appendDocumentStart(StringBuilder html, ReportMetadata metadata) {
        html.append("""
                <!doctype html>
                <html lang=\"en\">
                <head>
                  <meta charset=\"utf-8\" />
                  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />
                  <title>Verifier Analysis Report</title>
                  <style>
                    :root {
                      --bg: #f4f7f8;
                      --card: #ffffff;
                      --text: #112026;
                      --muted: #51666f;
                      --line: #d5dfe3;
                      --accent: #0e6b74;
                      --accent-soft: #e5f3f4;
                      --warn: #8a5300;
                      --warn-soft: #fff2de;
                    }
                    * { box-sizing: border-box; }
                    body {
                      margin: 0;
                      font-family: "IBM Plex Sans", "Source Sans 3", "Helvetica Neue", sans-serif;
                      color: var(--text);
                      background: radial-gradient(1200px 600px at 100% -10%, #d9ecef 0%, transparent 70%), var(--bg);
                      line-height: 1.4;
                    }
                    main {
                      max-width: 1240px;
                      margin: 0 auto;
                      padding: 22px 16px 28px;
                    }
                    .top {
                      background: linear-gradient(135deg, #123740 0%, #1f5864 100%);
                      color: #eff8fa;
                      border-radius: 14px;
                      padding: 18px 18px 14px;
                      box-shadow: 0 8px 24px rgba(16, 35, 42, 0.18);
                    }
                    .title {
                      margin: 0;
                      font-size: 1.35rem;
                      font-weight: 700;
                    }
                    .meta {
                      margin-top: 8px;
                      color: #d8edf2;
                      font-size: 0.9rem;
                    }
                    .meta code { color: #ffffff; }
                    .cards {
                      display: grid;
                      grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
                      gap: 10px;
                      margin-top: 14px;
                    }
                    .card {
                      background: rgba(30, 60, 70, 0.45);
                      border: 1px solid rgba(255, 255, 255, 0.2);
                      border-radius: 10px;
                      padding: 10px 12px;
                    }
                    .card .k { font-size: 0.74rem; color: #c4e0e8; text-transform: uppercase; letter-spacing: 0.04em; }
                    .card .v { margin-top: 2px; font-size: 1.12rem; font-weight: 700; color: #ffffff; text-shadow: 0 1px 1px rgba(0,0,0,0.3); }
                    details.section {
                      margin-top: 12px;
                      background: var(--card);
                      border: 1px solid var(--line);
                      border-radius: 12px;
                      overflow: hidden;
                    }
                    details.section > summary {
                      cursor: pointer;
                      list-style: none;
                      padding: 12px 14px;
                      font-weight: 600;
                      background: linear-gradient(180deg, #fbfeff 0%, #f5fbfc 100%);
                      border-bottom: 1px solid var(--line);
                    }
                    details.section[open] > summary {
                      background: linear-gradient(180deg, #f3fbfc 0%, #eaf5f7 100%);
                    }
                    details.section > summary::-webkit-details-marker { display: none; }
                    .section-content { padding: 12px 14px; }
                    details.subsection {
                      border: 1px solid var(--line);
                      border-radius: 10px;
                      margin-bottom: 10px;
                      background: #fcfefe;
                    }
                    details.subsection > summary {
                      cursor: pointer;
                      padding: 9px 11px;
                      font-weight: 600;
                      color: #25424b;
                      list-style: none;
                    }
                    details.subsection > summary::-webkit-details-marker { display: none; }
                    .subsection-content { padding: 0 11px 10px; }
                    table {
                      width: 100%;
                      border-collapse: collapse;
                      font-size: 0.92rem;
                    }
                    th, td {
                      border-bottom: 1px solid var(--line);
                      padding: 7px 8px;
                      text-align: left;
                      vertical-align: top;
                    }
                    th {
                      font-size: 0.78rem;
                      text-transform: uppercase;
                      color: var(--muted);
                      letter-spacing: 0.04em;
                    }
                    tr:last-child td { border-bottom: none; }
                    ul.clean { margin: 0; padding-left: 18px; }
                    .chip {
                      display: inline-block;
                      border-radius: 999px;
                      padding: 2px 8px;
                      font-size: 0.75rem;
                      font-weight: 600;
                      background: var(--accent-soft);
                      color: var(--accent);
                    }
                    .chip.warn { background: var(--warn-soft); color: var(--warn); }
                    .trace-pre {
                      margin: 0;
                      border: 1px solid var(--line);
                      border-radius: 8px;
                      background: #f8fbfc;
                      padding: 9px;
                      font-family: "IBM Plex Mono", "Source Code Pro", monospace;
                      font-size: 0.83rem;
                      white-space: pre-wrap;
                    }
                    .muted { color: var(--muted); }
                    .no-data {
                      color: var(--muted);
                      font-style: italic;
                      margin: 0;
                    }
                    code {
                      font-family: "IBM Plex Mono", "Source Code Pro", monospace;
                      font-size: 0.85em;
                    }
                  </style>
                </head>
                <body>
                <main>
                """);

        html.append("<section class=\"top\">\n");
        html.append("  <h1 class=\"title\">Verifier Analysis Report</h1>\n");
        html.append("  <div class=\"meta\">Generated <code>")
                .append(escapeHtml(metadata.generatedAtIso()))
                .append("</code> for <code>")
                .append(escapeHtml(metadata.applicationBaseDir()))
                .append("</code> under <code>")
                .append(escapeHtml(metadata.applicationsRoot()))
                .append("</code>.</div>\n");
        html.append("</section>\n");
    }

    private void appendSummaryCards(StringBuilder html,
                                    ApplicationAnalysisState state,
                                    Map<String, Integer> unresolvedByCategory) {
        int unresolvedTotal = unresolvedByCategory.values().stream().mapToInt(Integer::intValue).sum();
        html.append("<section class=\"cards\">\n");
        appendCard(html, "Dispatch targets", state.dispatchTargetFqns.size());
        appendCard(html, "Services", state.services.size());
        appendCard(html, "Command handlers", state.commandHandlers.size());
        appendCard(html, "Sagas", state.sagas.size());
        appendCard(html, "Creation sites", state.sagaCreationSites.size());
        appendCard(html, "Groovy constructor traces", state.groovyConstructorInputTraces.size());
        appendCard(html, "Groovy full traces", state.groovyFullTraceResults.size());
        appendCard(html, "Unresolved markers", unresolvedTotal);
        html.append("</section>\n");
    }

    private void appendCard(StringBuilder html, String key, int value) {
        html.append("<article class=\"card\"><div class=\"k\">")
                .append(escapeHtml(key))
                .append("</div><div class=\"v\">")
                .append(value)
                .append("</div></article>\n");
    }

    private void appendGroovySection(StringBuilder html,
                                     List<GroovyConstructorInputTrace> constructorTraces,
                                     List<GroovyTraceView> fullTraceViews,
                                     Map<String, Integer> unresolvedByCategory) {
        html.append("<details class=\"section\" open><summary>Groovy Trace Explorer</summary><div class=\"section-content\">\n");

        html.append("<details class=\"subsection\" open><summary>Summary to detailed: constructor-input traces (")
                .append(constructorTraces.size())
                .append(")</summary><div class=\"subsection-content\">\n");

        if (constructorTraces.isEmpty()) {
            html.append("<p class=\"no-data\">No constructor traces found.</p>\n");
        } else {
            html.append("<table><thead><tr><th>Source class</th><th>Method</th><th>Saga constructor</th></tr></thead><tbody>\n");
            constructorTraces.forEach(trace -> {
                html.append("<tr><td><code>")
                        .append(escapeHtml(simpleName(trace.sourceClassFqn())))
                        .append("</code></td><td><code>")
                        .append(escapeHtml(trace.sourceMethodName()))
                        .append("()</code></td><td><code>")
                        .append(escapeHtml(simpleName(trace.sagaClassFqn())))
                        .append("</code></td></tr>\n");
            });
            html.append("</tbody></table>\n");
        }
        html.append("</div></details>\n");

        html.append("<details class=\"subsection\"><summary>Detailed to deeper: unresolved input markers (")
                .append(unresolvedByCategory.values().stream().mapToInt(Integer::intValue).sum())
                .append(")</summary><div class=\"subsection-content\">\n");
        if (unresolvedByCategory.isEmpty()) {
            html.append("<p class=\"no-data\">No unresolved markers found in Groovy full traces.</p>\n");
        } else {
            html.append("<table><thead><tr><th>Marker category</th><th>Count</th></tr></thead><tbody>\n");
            unresolvedByCategory.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                            .thenComparing(Map.Entry.comparingByKey()))
                    .forEach(entry -> html.append("<tr><td><span class=\"chip warn\">")
                            .append(escapeHtml(entry.getKey()))
                            .append("</span></td><td>")
                            .append(entry.getValue())
                            .append("</td></tr>\n"));
            html.append("</tbody></table>\n");
        }
        html.append("</div></details>\n");

        html.append("<details class=\"subsection\"><summary>Full trace details (")
                .append(fullTraceViews.size())
                .append(")</summary><div class=\"subsection-content\">\n");
        if (fullTraceViews.isEmpty()) {
            html.append("<p class=\"no-data\">No full traces found.</p>\n");
        } else {
            for (int i = 0; i < fullTraceViews.size(); i++) {
                GroovyTraceView trace = fullTraceViews.get(i);
                html.append("<details class=\"subsection\"><summary>")
                        .append("<code>")
                        .append(escapeHtml(simpleName(trace.sourceClassFqn())))
                        .append(".</code><code>")
                        .append(escapeHtml(trace.sourceMethodName()))
                        .append("()</code> -> <code>")
                        .append(escapeHtml(simpleName(trace.sagaClassFqn())))
                        .append("</code> &nbsp; ")
                        .append("<span class=\"chip\">args: ")
                        .append(trace.maxArgIndex() < 0 ? 0 : (trace.maxArgIndex() + 1))
                        .append("</span> ")
                        .append("<span class=\"chip warn\">unresolved: ")
                        .append(trace.unresolvedMarkers().size())
                        .append("</span>")
                        .append("</summary><div class=\"subsection-content\">\n");

                if (!trace.unresolvedMarkers().isEmpty()) {
                    html.append("<p class=\"muted\">Unresolved markers in this trace: ");
                    html.append(trace.unresolvedMarkers().stream()
                            .collect(Collectors.groupingBy(s -> s, LinkedHashMap::new, Collectors.counting()))
                            .entrySet().stream()
                            .map(entry -> "<span class=\"chip warn\">" +
                                    escapeHtml(entry.getKey()) + " x" + entry.getValue() + "</span>")
                            .collect(Collectors.joining(" ")));
                    html.append("</p>\n");
                }

                html.append("<pre class=\"trace-pre\">")
                        .append(escapeHtml(trace.traceText()))
                        .append("</pre>\n");
                html.append("</div></details>\n");
            }
        }
        html.append("</div></details>\n");

        html.append("</div></details>\n");
    }

    private void appendSagasSection(StringBuilder html, ApplicationAnalysisState state) {
        html.append("<details class=\"section\" open><summary>Sagas and Steps (")
                .append(state.sagas.size())
                .append(")</summary><div class=\"section-content\">\n");

        if (state.sagas.isEmpty()) {
            html.append("<p class=\"no-data\">No sagas were discovered.</p>\n");
            html.append("</div></details>\n");
            return;
        }

        state.sagas.stream()
                .sorted(Comparator.comparing(SagaFunctionalityBuildingBlock::getFqn))
                .forEach(saga -> {
                    html.append("<details class=\"subsection\"><summary><code>")
                            .append(escapeHtml(simpleName(saga.getFqn())))
                            .append("</code> <span class=\"chip\">steps: ")
                            .append(saga.getSteps().size())
                            .append("</span></summary><div class=\"subsection-content\">\n");
                    if (saga.getSteps().isEmpty()) {
                        html.append("<p class=\"no-data\">No steps found for this saga.</p>\n");
                    } else {
                        saga.getSteps().forEach(step -> appendSagaStep(html, step));
                    }
                    html.append("</div></details>\n");
                });

        html.append("</div></details>\n");
    }

    private void appendSagaStep(StringBuilder html, SagaStepBuildingBlock step) {
        html.append("<details class=\"subsection\"><summary><code>")
                .append(escapeHtml(step.getName()))
                .append("</code> <span class=\"chip\">dispatches: ")
                .append(step.getDispatches().size())
                .append("</span></summary><div class=\"subsection-content\">\n");

        if (step.getPredecessorStepKeys().isEmpty()) {
            html.append("<p class=\"muted\">Predecessors: (none)</p>\n");
        } else {
            html.append("<p class=\"muted\">Predecessors: <code>")
                    .append(escapeHtml(String.join(", ", step.getPredecessorStepKeys())))
                    .append("</code></p>\n");
        }

        if (step.getDispatches().isEmpty()) {
            html.append("<p class=\"no-data\">No dispatches found in this step.</p>\n");
            html.append("</div></details>\n");
            return;
        }

        html.append("<table><thead><tr><th>Command</th><th>Aggregate</th><th>Access</th><th>Phase</th><th>Multiplicity</th></tr></thead><tbody>\n");
        for (StepDispatchFootprint dispatch : step.getDispatches()) {
            html.append("<tr><td><code>")
                    .append(escapeHtml(simpleName(dispatch.commandTypeFqn())))
                    .append("</code></td><td><code>")
                    .append(escapeHtml(dispatch.aggregateName()))
                    .append("</code></td><td>")
                    .append(escapeHtml(String.valueOf(dispatch.accessPolicy())))
                    .append("</td><td>")
                    .append(escapeHtml(String.valueOf(dispatch.phase())))
                    .append("</td><td>")
                    .append(escapeHtml(formatMultiplicity(dispatch.multiplicity())))
                    .append("</td></tr>\n");
        }
        html.append("</tbody></table>\n");
        html.append("</div></details>\n");
    }

    private void appendCommandHandlersSection(StringBuilder html, ApplicationAnalysisState state) {
        html.append("<details class=\"section\"><summary>Command Handlers (")
                .append(state.commandHandlers.size())
                .append(")</summary><div class=\"section-content\">\n");

        if (state.commandHandlers.isEmpty()) {
            html.append("<p class=\"no-data\">No command handlers were discovered.</p>\n");
            html.append("</div></details>\n");
            return;
        }

        state.commandHandlers.stream()
                .sorted(Comparator.comparing(CommandHandlerBuildingBlock::getFqn))
                .forEach(handler -> {
                    html.append("<details class=\"subsection\"><summary><code>")
                            .append(escapeHtml(simpleName(handler.getFqn())))
                            .append("</code>");
                    if (handler.getAggregateTypeName() != null) {
                        html.append(" <span class=\"chip\">aggregate: ")
                                .append(escapeHtml(handler.getAggregateTypeName()))
                                .append("</span>");
                    }
                    html.append("</summary><div class=\"subsection-content\">\n");

                    if (handler.getCommandDispatch().isEmpty()) {
                        html.append("<p class=\"no-data\">No command dispatches mapped for this handler.</p>\n");
                    } else {
                        html.append("<table><thead><tr><th>Command</th><th>Service method</th><th>Access</th></tr></thead><tbody>\n");
                        handler.getCommandDispatch().entrySet().stream()
                                .sorted(Map.Entry.comparingByKey())
                                .forEach(entry -> {
                                    CommandDispatchInfo dispatch = entry.getValue();
                                    html.append("<tr><td><code>")
                                            .append(escapeHtml(simpleName(entry.getKey())))
                                            .append("</code></td><td><code>")
                                            .append(escapeHtml(simpleName(dispatch.serviceClassName())))
                                            .append(".</code><code>")
                                            .append(escapeHtml(dispatch.serviceMethodSignature()))
                                            .append("</code></td><td>")
                                            .append(escapeHtml(String.valueOf(dispatch.accessPolicy())))
                                            .append("</td></tr>\n");
                                });
                        html.append("</tbody></table>\n");
                    }

                    html.append("</div></details>\n");
                });

        html.append("</div></details>\n");
    }

    private void appendServicesSection(StringBuilder html, ApplicationAnalysisState state) {
        html.append("<details class=\"section\"><summary>Services (")
                .append(state.services.size())
                .append(")</summary><div class=\"section-content\">\n");

        if (state.services.isEmpty()) {
            html.append("<p class=\"no-data\">No services were discovered.</p>\n");
            html.append("</div></details>\n");
            return;
        }

        state.services.stream()
                .sorted(Comparator.comparing(ServiceBuildingBlock::getFqn))
                .forEach(service -> {
                    html.append("<details class=\"subsection\"><summary><code>")
                            .append(escapeHtml(simpleName(service.getFqn())))
                            .append("</code> <span class=\"chip\">methods: ")
                            .append(service.getMethodAccessPolicies().size())
                            .append("</span></summary><div class=\"subsection-content\">\n");
                    if (service.getMethodAccessPolicies().isEmpty()) {
                        html.append("<p class=\"no-data\">No public service methods discovered.</p>\n");
                    } else {
                        html.append("<ul class=\"clean\">\n");
                        service.getMethodAccessPolicies().entrySet().stream()
                                .sorted(Map.Entry.comparingByKey())
                                .forEach(entry -> html.append("<li><code>")
                                        .append(escapeHtml(entry.getKey()))
                                        .append("</code> <span class=\"chip\">")
                                        .append(escapeHtml(String.valueOf(entry.getValue())))
                                        .append("</span></li>\n"));
                        html.append("</ul>\n");
                    }
                    html.append("</div></details>\n");
                });

        html.append("</div></details>\n");
    }

    private void appendDispatchTargetsSection(StringBuilder html, ApplicationAnalysisState state) {
        html.append("<details class=\"section\"><summary>Dispatch Targets (")
                .append(state.dispatchTargetFqns.size())
                .append(")</summary><div class=\"section-content\">\n");

        if (state.dispatchTargetFqns.isEmpty()) {
            html.append("<p class=\"no-data\">No dispatch targets found.</p>\n");
        } else {
            html.append("<ul class=\"clean\">\n");
            state.dispatchTargetFqns.stream().sorted().forEach(target ->
                    html.append("<li><code>")
                            .append(escapeHtml(target))
                            .append("</code></li>\n"));
            html.append("</ul>\n");
        }

        html.append("</div></details>\n");
    }

    private void appendCreationSitesSection(StringBuilder html, ApplicationAnalysisState state) {
        html.append("<details class=\"section\"><summary>Saga Creation Sites (")
                .append(state.sagaCreationSites.size())
                .append(")</summary><div class=\"section-content\">\n");

        if (state.sagaCreationSites.isEmpty()) {
            html.append("<p class=\"no-data\">No saga creation sites found.</p>\n");
        } else {
            html.append("<table><thead><tr><th>Class</th><th>Method</th><th>Saga</th></tr></thead><tbody>\n");
            state.sagaCreationSites.stream()
                    .sorted(Comparator.comparing(WorkflowFunctionalityCreationSite::classFqn)
                            .thenComparing(WorkflowFunctionalityCreationSite::methodName)
                            .thenComparing(WorkflowFunctionalityCreationSite::sagaClassFqn))
                    .forEach(site -> html.append("<tr><td><code>")
                            .append(escapeHtml(simpleName(site.classFqn())))
                            .append("</code></td><td><code>")
                            .append(escapeHtml(site.methodName()))
                            .append("()</code></td><td><code>")
                            .append(escapeHtml(simpleName(site.sagaClassFqn())))
                            .append("</code></td></tr>\n"));
            html.append("</tbody></table>\n");
        }

        html.append("</div></details>\n");
    }

    private void appendInterfaceIndexSection(StringBuilder html, ApplicationAnalysisState state) {
        html.append("<details class=\"section\"><summary>Interface to Service Index (")
                .append(state.interfaceToServices.size())
                .append(")</summary><div class=\"section-content\">\n");

        if (state.interfaceToServices.isEmpty()) {
            html.append("<p class=\"no-data\">No interface mappings discovered.</p>\n");
            html.append("</div></details>\n");
            return;
        }

        html.append("<table><thead><tr><th>Interface</th><th>Service implementations</th></tr></thead><tbody>\n");
        state.interfaceToServices.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String implementations = entry.getValue().stream()
                            .map(ServiceBuildingBlock::getFqn)
                            .sorted()
                            .map(this::simpleName)
                            .collect(Collectors.joining(", "));
                    html.append("<tr><td><code>")
                            .append(escapeHtml(entry.getKey()))
                            .append("</code></td><td><code>")
                            .append(escapeHtml(implementations))
                            .append("</code></td></tr>\n");
                });
        html.append("</tbody></table>\n");
        html.append("</div></details>\n");
    }

    private void appendRawTextSection(StringBuilder html, String rawTextReport) {
        html.append("<details class=\"section\"><summary>Raw Text Report (verbatim)</summary><div class=\"section-content\">\n");
        html.append("<pre class=\"trace-pre\">")
                .append(escapeHtml(rawTextReport == null ? "" : rawTextReport))
                .append("</pre>\n");
        html.append("</div></details>\n");
    }

    private void appendDocumentEnd(StringBuilder html) {
        html.append("</main></body></html>");
    }

    private GroovyTraceView toGroovyTraceView(GroovyFullTraceResult traceResult) {
        String traceText = traceResult.traceText() == null ? "" : traceResult.traceText();
        List<String> unresolved = new ArrayList<>();
        int maxArgIndex = -1;

        for (String line : traceText.lines().toList()) {
            Matcher argMatcher = ARG_INDEX_PATTERN.matcher(line);
            if (argMatcher.find()) {
                int index = Integer.parseInt(argMatcher.group(1));
                if (index > maxArgIndex) {
                    maxArgIndex = index;
                }
            }

            Matcher unresolvedMatcher = UNRESOLVED_NOTE_PATTERN.matcher(line);
            while (unresolvedMatcher.find()) {
                unresolved.add(unresolvedMatcher.group(1));
            }
        }

        return new GroovyTraceView(
                traceResult.sourceClassFqn(),
                traceResult.sourceMethodName(),
                traceResult.sagaClassFqn(),
                traceText,
                maxArgIndex,
                unresolved
        );
    }

    private Map<String, Integer> collectUnresolvedByCategory(List<GroovyTraceView> traceViews) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        traceViews.stream()
                .flatMap(trace -> trace.unresolvedMarkers().stream())
                .forEach(marker -> counts.put(marker, counts.getOrDefault(marker, 0) + 1));
        return counts;
    }

    private String formatMultiplicity(DispatchMultiplicity multiplicity) {
        if (multiplicity == null) {
            return "(unknown)";
        }

        if (multiplicity.staticCount() == null) {
            return multiplicity.kind().name();
        }

        return multiplicity.kind().name() + " x" + multiplicity.staticCount();
    }

    private String simpleName(String fqn) {
        if (fqn == null || fqn.isBlank()) {
            return "(unknown)";
        }

        int lastDot = fqn.lastIndexOf('.');
        return lastDot < 0 ? fqn : fqn.substring(lastDot + 1);
    }

    private static String escapeHtml(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                case '"' -> escaped.append("&quot;");
                case '\'' -> escaped.append("&#39;");
                default -> escaped.append(ch);
            }
        }
        return escaped.toString();
    }

    public record ReportMetadata(String applicationsRoot,
                                 String applicationBaseDir,
                                 String generatedAtIso) {

        public static ReportMetadata now(String applicationsRoot, String applicationBaseDir) {
            return new ReportMetadata(
                    applicationsRoot,
                    applicationBaseDir,
                    OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            );
        }
    }

    private record GroovyTraceView(String sourceClassFqn,
                                   String sourceMethodName,
                                   String sagaClassFqn,
                                   String traceText,
                                   int maxArgIndex,
                                   List<String> unresolvedMarkers) {
    }
}

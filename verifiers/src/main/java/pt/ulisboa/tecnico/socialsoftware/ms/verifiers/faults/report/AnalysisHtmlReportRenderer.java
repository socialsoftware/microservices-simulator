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
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyTraceArgument;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyFullTraceResult;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyTraceOriginKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueRecipe;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyWorkflowCall;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AnalysisHtmlReportRenderer {
    private static final Pattern UNRESOLVED_NOTE_PATTERN = Pattern.compile("\\[(unresolved [^\\]]+)\\]");

    public String render(ApplicationAnalysisState state,
                         ReportMetadata metadata,
                         String rawTextReport) {
        StringBuilder html = new StringBuilder(32_000);

        List<GroovyConstructorInputTrace> constructorTraces = state.groovyConstructorInputTraces.stream()
                .sorted(Comparator.comparing(GroovyConstructorInputTrace::sourceClassFqn)
                        .thenComparing(GroovyConstructorInputTrace::sourceMethodName)
                        .thenComparing(trace -> trace.sourceBindingName() == null ? "" : trace.sourceBindingName())
                        .thenComparing(GroovyConstructorInputTrace::sagaClassFqn))
                .toList();

        List<GroovyFullTraceResult> fullTraceResults = state.groovyFullTraceResults.stream()
                .sorted(Comparator.comparing(GroovyFullTraceResult::sourceClassFqn)
                        .thenComparing(GroovyFullTraceResult::sourceMethodName)
                        .thenComparing(trace -> trace.sourceBindingName() == null ? "" : trace.sourceBindingName())
                        .thenComparing(GroovyFullTraceResult::sagaClassFqn))
                .toList();
        List<GroovyTraceView> fullTraceViews = toGroovyTraceViews(fullTraceResults);

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
                    a {
                      color: var(--accent);
                      text-decoration: none;
                    }
                    a:hover {
                      text-decoration: underline;
                    }
                    .trace-card {
                      border: 1px solid var(--line);
                      border-radius: 10px;
                      background: #ffffff;
                      padding: 11px;
                      margin-bottom: 10px;
                    }
                    .trace-story {
                      margin: 0 0 7px;
                      font-size: 0.95rem;
                    }
                    .trace-chip-row {
                      margin: 0 0 8px;
                      display: flex;
                      flex-wrap: wrap;
                      gap: 6px;
                    }
                    .severity-high { background: #ffe4e4; color: #8d2020; }
                    .severity-medium { background: #fff2de; color: #8a5300; }
                    .severity-low { background: #e8f2ff; color: #1b4f86; }
                    .trace-mini-graph {
                      width: 100%;
                      max-width: 780px;
                      border: 1px solid var(--line);
                      border-radius: 8px;
                      background: #fbfeff;
                      margin: 8px 0;
                    }
                    .trace-mini-graph text {
                      font-family: "IBM Plex Sans", "Source Sans 3", sans-serif;
                      font-size: 11px;
                      fill: #17333c;
                    }
                    .legacy-trace-hook {
                      display: none;
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
            html.append("<table><thead><tr><th>Source class</th><th>Method</th><th>Binding</th><th>Saga constructor</th></tr></thead><tbody>\n");
            constructorTraces.forEach(trace -> {
                html.append("<tr><td><code>")
                        .append(escapeHtml(simpleName(trace.sourceClassFqn())))
                        .append("</code></td><td><code>")
                        .append(escapeHtml(trace.sourceMethodName()))
                        .append("()</code></td><td><code>")
                        .append(escapeHtml(trace.sourceBindingName() == null ? "(unknown)" : trace.sourceBindingName()))
                        .append("</code></td><td><code>")
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

        appendGroupedGroovyTraceViews(html, fullTraceViews);
        appendCanonicalTraceCards(html, fullTraceViews);

        html.append("</div></details>\n");
    }

    private void appendGroupedGroovyTraceViews(StringBuilder html, List<GroovyTraceView> fullTraceViews) {
        Map<String, List<GroovyTraceView>> bySourceMethod = fullTraceViews.stream()
                .collect(Collectors.groupingBy(this::formatSourceMethodLabel, LinkedHashMap::new, Collectors.toList()));
        Map<String, List<GroovyTraceView>> bySagaTarget = fullTraceViews.stream()
                .collect(Collectors.groupingBy(trace -> simpleName(trace.sagaClassFqn()), LinkedHashMap::new, Collectors.toList()));
        Map<String, List<GroovyTraceView>> byUnresolvedMarker = new LinkedHashMap<>();

        for (GroovyTraceView trace : fullTraceViews) {
            for (String marker : new LinkedHashSet<>(trace.unresolvedMarkers())) {
                byUnresolvedMarker.computeIfAbsent(marker, ignored -> new ArrayList<>()).add(trace);
            }
        }

        appendGroupedTraceSubsection(html, "Grouped by source method", bySourceMethod, null);
        appendGroupedTraceSubsection(html, "Grouped by saga target", bySagaTarget, null);

        Map<String, Integer> unresolvedMarkerCounts = fullTraceViews.stream()
                .flatMap(trace -> trace.unresolvedMarkers().stream())
                .collect(Collectors.toMap(marker -> marker, marker -> 1, Integer::sum, LinkedHashMap::new));
        appendGroupedTraceSubsection(html, "Grouped by unresolved marker category", byUnresolvedMarker, unresolvedMarkerCounts);
    }

    private void appendGroupedTraceSubsection(StringBuilder html,
                                              String title,
                                              Map<String, List<GroovyTraceView>> groupedTraces,
                                              Map<String, Integer> markerCounts) {
        html.append("<details class=\"subsection\" open><summary>")
                .append(escapeHtml(title))
                .append("</summary><div class=\"subsection-content\">\n");

        if (groupedTraces.isEmpty()) {
            html.append("<p class=\"no-data\">No traces found.</p>\n");
            html.append("</div></details>\n");
            return;
        }

        groupedTraces.forEach((groupKey, traces) -> {
            html.append("<details class=\"subsection\"><summary><code>")
                    .append(escapeHtml(groupKey))
                    .append("</code> <span class=\"chip\">trace(s): ")
                    .append(traces.size())
                    .append("</span>");

            if (markerCounts != null) {
                int markerCount = markerCounts.getOrDefault(groupKey, traces.size());
                html.append(" <span class=\"chip warn\">marker(s): ")
                        .append(markerCount)
                        .append("</span>");
            }

            html.append("</summary><div class=\"subsection-content\">\n<ul class=\"clean\">\n");
            traces.forEach(trace -> html.append("<li><a href=\"#")
                    .append(escapeHtml(trace.traceCardId()))
                    .append("\"><code>")
                    .append(escapeHtml(formatSourceMethodLabel(trace)))
                    .append("</code> -> <code>")
                    .append(escapeHtml(simpleName(trace.sagaClassFqn())))
                    .append("</code></a></li>\n"));
            html.append("</ul>\n</div></details>\n");
        });

        html.append("</div></details>\n");
    }

    private void appendCanonicalTraceCards(StringBuilder html, List<GroovyTraceView> fullTraceViews) {
        html.append("<details class=\"subsection\" open><summary>Canonical trace cards (")
                .append(fullTraceViews.size())
                .append(")</summary><div class=\"subsection-content\">\n");

        if (fullTraceViews.isEmpty()) {
            html.append("<p class=\"no-data\">No full traces found.</p>\n");
        } else {
            fullTraceViews.forEach(trace -> appendGroovyTraceCard(html, trace));
        }

        html.append("</div></details>\n");
    }

    private void appendGroovyTraceCard(StringBuilder html, GroovyTraceView trace) {
        int argumentCount = constructorArgumentCount(trace.constructorArguments());

        html.append("<article class=\"trace-card\" id=\"")
                .append(escapeHtml(trace.traceCardId()))
                .append("\">\n");

        html.append("<h4 class=\"trace-story\">Story: <code>")
                .append(escapeHtml(formatSourceMethodLabel(trace)))
                .append("</code> constructs <code>")
                .append(escapeHtml(simpleName(trace.sagaClassFqn())))
                .append("</code> with ")
                .append(argumentCount)
                .append(" argument(s) and ")
                .append(trace.unresolvedMarkers().size())
                .append(" unresolved marker(s).</h4>\n");

        html.append("<p class=\"trace-chip-row\">")
                .append("<span class=\"chip\">origin: ")
                .append(escapeHtml(formatOriginLabel(trace.originKind())))
                .append("</span>")
                .append("<span class=\"chip\">args: ")
                .append(argumentCount)
                .append("</span>")
                .append("<span class=\"chip warn\">unresolved: ")
                .append(trace.unresolvedMarkers().size())
                .append("</span>");
        if (trace.sourceBindingName() != null && !trace.sourceBindingName().isBlank()) {
            html.append("<span class=\"chip\">binding: ")
                    .append(escapeHtml(trace.sourceBindingName()))
                    .append("</span>");
        }
        html.append("</p>\n");

        if (!trace.unresolvedCategories().isEmpty()) {
            html.append("<p class=\"trace-chip-row\">\n");
            trace.unresolvedCategories().forEach(category -> html.append("<span class=\"chip ")
                    .append(escapeHtml(category.severityClass()))
                    .append("\">Severity: ")
                    .append(escapeHtml(category.normalizedLabel()))
                    .append(" (")
                    .append(escapeHtml(category.severityLevel()))
                    .append(")")
                    .append(category.occurrences() > 1 ? " x" + category.occurrences() : "")
                    .append("</span>\n"));
            html.append("</p>\n");
        }

        if (trace.sourceExpressionText() != null && !trace.sourceExpressionText().isBlank()) {
            html.append("<p class=\"muted\">Source expression: <code>")
                    .append(escapeHtml(trace.sourceExpressionText()))
                    .append("</code></p>\n");
        }

        appendTraceMiniGraph(html, trace);
        appendTraceArgumentsTable(html, trace.constructorArguments());
        appendWorkflowBreadcrumbs(html, trace.workflowCalls());
        appendResolutionNotes(html, trace.resolutionNotes());

        String legacyMethodName = (trace.sourceMethodName() == null || trace.sourceMethodName().isBlank())
                ? "(unknown)"
                : trace.sourceMethodName();
        html.append("<p class=\"legacy-trace-hook\" data-role=\"legacy-trace-hook\"><code>")
                .append(escapeHtml(simpleName(trace.sourceClassFqn())))
                .append(".</code><code>")
                .append(escapeHtml(legacyMethodName))
                .append("()</code></p>\n");

        html.append("<details class=\"subsection trace-raw\"><summary>Raw trace block</summary><div class=\"subsection-content\">\n")
                .append("<pre class=\"trace-pre\">")
                .append(escapeHtml(trace.traceText()))
                .append("</pre>\n")
                .append("</div></details>\n");

        html.append("</article>\n");
    }

    private void appendTraceMiniGraph(StringBuilder html, GroovyTraceView trace) {
        String sourceLabel = trace.sourceExpressionText() == null || trace.sourceExpressionText().isBlank()
                ? formatSourceMethodLabel(trace)
                : trace.sourceExpressionText();
        String sourceGraphLabel = trimForGraphLabel(sourceLabel);
        String sagaGraphLabel = trimForGraphLabel("new " + simpleName(trace.sagaClassFqn()) + "(...)");

        html.append("<svg class=\"trace-mini-graph\" viewBox=\"0 0 760 106\" role=\"img\" id=\"")
                .append(escapeHtml(trace.miniGraphId()))
                .append("\" data-trace-card-ref=\"")
                .append(escapeHtml(trace.traceCardId()))
                .append("\" aria-label=\"Trace mini graph\">\n")
                .append("<rect x=\"14\" y=\"18\" width=\"300\" height=\"54\" rx=\"8\" fill=\"#eef7f9\" stroke=\"#b8d3d8\" />\n")
                .append("<text x=\"28\" y=\"40\">source expression</text>\n")
                .append("<text x=\"28\" y=\"58\">")
                .append(escapeHtml(sourceGraphLabel))
                .append("</text>\n")
                .append("<line x1=\"326\" y1=\"45\" x2=\"436\" y2=\"45\" stroke=\"#5a7880\" stroke-width=\"2\" />\n")
                .append("<polygon points=\"436,45 426,39 426,51\" fill=\"#5a7880\" />\n")
                .append("<rect x=\"448\" y=\"18\" width=\"298\" height=\"54\" rx=\"8\" fill=\"#edf7ee\" stroke=\"#b9d8bb\" />\n")
                .append("<text x=\"462\" y=\"40\">saga constructor</text>\n")
                .append("<text x=\"462\" y=\"58\">")
                .append(escapeHtml(sagaGraphLabel))
                .append("</text>\n")
                .append("</svg>\n");
    }

    private void appendTraceArgumentsTable(StringBuilder html, List<GroovyTraceArgument> constructorArguments) {
        html.append("<table data-role=\"trace-arguments\"><thead><tr><th>Arg</th><th>Provenance</th><th>Value recipe</th></tr></thead><tbody>\n");
        if (constructorArguments.isEmpty()) {
            html.append("<tr><td colspan=\"3\" class=\"muted\">No constructor arguments were traced.</td></tr>\n");
        } else {
            constructorArguments.stream()
                    .sorted(Comparator.comparingInt(GroovyTraceArgument::index))
                    .forEach(argument -> {
                        html.append("<tr><td>")
                                .append("arg[")
                                .append(argument.index())
                                .append("]")
                                .append("</td><td>")
                                .append(escapeHtml(argument.provenance() == null ? "(unknown)" : argument.provenance()))
                                .append("</td><td>");
                        appendValueRecipe(html, argument.recipe());
                        html.append("</td></tr>\n");
                    });
        }
        html.append("</tbody></table>\n");
    }

    private void appendValueRecipe(StringBuilder html, GroovyValueRecipe recipe) {
        if (recipe == null) {
            html.append("<span class=\"muted\">(none)</span>");
            return;
        }

        html.append("<code>")
                .append(escapeHtml(recipe.kind() == null ? "UNKNOWN" : recipe.kind().name()))
                .append(": ")
                .append(escapeHtml(recipe.text() == null ? "(empty)" : recipe.text()))
                .append("</code>");

        List<GroovyValueRecipe> children = recipe.children() == null ? List.of() : recipe.children();
        if (!children.isEmpty()) {
            html.append("<ul class=\"clean\">\n");
            children.forEach(child -> {
                html.append("<li>");
                appendValueRecipe(html, child);
                html.append("</li>\n");
            });
            html.append("</ul>\n");
        }
    }

    private void appendWorkflowBreadcrumbs(StringBuilder html, List<GroovyWorkflowCall> workflowCalls) {
        html.append("<section data-role=\"workflow-breadcrumbs\">\n")
                .append("<h5>Workflow breadcrumbs</h5>\n");
        if (workflowCalls.isEmpty()) {
            html.append("<p class=\"no-data\">No workflow calls recorded.</p>\n");
        } else {
            html.append("<ul class=\"clean\">\n");
            workflowCalls.forEach(call -> html.append("<li><code>")
                    .append(escapeHtml(call.callText() == null ? "(unknown call)" : call.callText()))
                    .append("</code>")
                    .append(" <span class=\"chip\">")
                    .append(escapeHtml(call.contextLabel() == null || call.contextLabel().isBlank()
                            ? "(context unknown)"
                            : call.contextLabel()))
                    .append("</span></li>\n"));
            html.append("</ul>\n");
        }
        html.append("</section>\n");
    }

    private void appendResolutionNotes(StringBuilder html, List<String> resolutionNotes) {
        html.append("<section data-role=\"resolution-notes\">\n")
                .append("<h5>Resolution notes</h5>\n");
        if (resolutionNotes.isEmpty()) {
            html.append("<p class=\"no-data\">No resolution notes.</p>\n");
        } else {
            html.append("<ul class=\"clean\">\n");
            resolutionNotes.forEach(note -> html.append("<li>")
                    .append(escapeHtml(note))
                    .append("</li>\n"));
            html.append("</ul>\n");
        }
        html.append("</section>\n");
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

    private List<GroovyTraceView> toGroovyTraceViews(List<GroovyFullTraceResult> traceResults) {
        Map<String, Integer> seenBySlug = new LinkedHashMap<>();
        List<GroovyTraceView> views = new ArrayList<>();

        for (GroovyFullTraceResult traceResult : traceResults) {
            String baseSlug = buildTraceSlug(traceResult.sourceClassFqn(), traceResult.sourceMethodName(), traceResult.sagaClassFqn());
            int seen = seenBySlug.getOrDefault(baseSlug, 0);
            seenBySlug.put(baseSlug, seen + 1);
            String traceSlug = seen == 0 ? baseSlug : baseSlug + "-" + (seen + 1);
            views.add(toGroovyTraceView(traceResult, traceSlug));
        }

        return views;
    }

    private GroovyTraceView toGroovyTraceView(GroovyFullTraceResult traceResult, String traceSlug) {
        String traceText = traceResult.traceText() == null ? "" : traceResult.traceText();
        List<GroovyTraceArgument> constructorArguments =
                traceResult.constructorArguments() == null ? List.of() : traceResult.constructorArguments();
        List<GroovyWorkflowCall> workflowCalls =
                traceResult.workflowCalls() == null ? List.of() : traceResult.workflowCalls();
        List<String> resolutionNotes =
                traceResult.resolutionNotes() == null ? List.of() : traceResult.resolutionNotes();

        List<String> unresolved = new ArrayList<>();
        constructorArguments.forEach(argument -> collectUnresolvedMarkers(argument.provenance(), unresolved));
        resolutionNotes.forEach(note -> collectUnresolvedMarkers(note, unresolved));

        if (unresolved.isEmpty()) {
            traceText.lines().forEach(line -> collectUnresolvedMarkers(line, unresolved));
        }

        List<UnresolvedCategoryView> unresolvedCategories = unresolved.stream()
                .collect(Collectors.groupingBy(marker -> marker, LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> toUnresolvedCategoryView(entry.getKey(), entry.getValue().intValue()))
                .toList();

        return new GroovyTraceView(
                traceResult.sourceClassFqn(),
                traceResult.sourceMethodName(),
                traceResult.sourceBindingName(),
                traceResult.originKind(),
                traceResult.sourceExpressionText(),
                traceResult.sagaClassFqn(),
                traceText,
                constructorArguments,
                workflowCalls,
                resolutionNotes,
                unresolved,
                unresolvedCategories,
                traceCardId(traceSlug),
                miniGraphId(traceSlug)
        );
    }

    private UnresolvedCategoryView toUnresolvedCategoryView(String rawMarker, int occurrences) {
        String normalized = normalizeUnresolvedCategory(rawMarker);
        String lower = normalized.toLowerCase();

        if (lower.contains("runtime edge")) {
            return new UnresolvedCategoryView(rawMarker, normalized, "high", "severity-high", occurrences);
        }
        if (lower.contains("cyclic reference")) {
            return new UnresolvedCategoryView(rawMarker, normalized, "high", "severity-high", occurrences);
        }
        if (lower.contains("source-backed")) {
            return new UnresolvedCategoryView(rawMarker, normalized, "medium", "severity-medium", occurrences);
        }

        return new UnresolvedCategoryView(rawMarker, normalized, "low", "severity-low", occurrences);
    }

    private String normalizeUnresolvedCategory(String rawMarker) {
        if (rawMarker == null || rawMarker.isBlank()) {
            return "(unknown unresolved)";
        }

        String trimmed = rawMarker.trim();
        String unresolvedPrefix = "unresolved ";
        if (trimmed.startsWith(unresolvedPrefix)) {
            return trimmed.substring(unresolvedPrefix.length());
        }
        return trimmed;
    }

    private String formatSourceMethodLabel(GroovyTraceView trace) {
        return simpleName(trace.sourceClassFqn()) + "." +
                ((trace.sourceMethodName() == null || trace.sourceMethodName().isBlank()) ? "(unknown)" : trace.sourceMethodName()) +
                "()";
    }

    private int constructorArgumentCount(List<GroovyTraceArgument> constructorArguments) {
        return constructorArguments.stream()
                .mapToInt(GroovyTraceArgument::index)
                .max()
                .orElse(-1) + 1;
    }

    private String traceCardId(String traceSlug) {
        return "trace-card-" + traceSlug;
    }

    private String miniGraphId(String traceSlug) {
        return "trace-mini-graph-" + traceSlug;
    }

    private String buildTraceSlug(String sourceClassFqn, String sourceMethodName, String sagaClassFqn) {
        String sourceClassPart = toKebabToken(simpleName(sourceClassFqn));
        String sourceMethodPart = toKebabToken(sourceMethodName == null ? "unknown" : sourceMethodName);
        String sagaPart = toKebabToken(simpleName(sagaClassFqn));
        return sourceClassPart + "-" + sourceMethodPart + "-" + sagaPart;
    }

    private String toKebabToken(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }

        String withWordBoundaries = value.replaceAll("([a-z0-9])([A-Z])", "$1-$2")
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1-$2");
        String normalized = withWordBoundaries
                .replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "")
                .toLowerCase();
        return normalized.isBlank() ? "unknown" : normalized;
    }

    private String trimForGraphLabel(String text) {
        if (text == null || text.isBlank()) {
            return "(unknown)";
        }

        String collapsed = text.replace('\n', ' ').replaceAll("\\s+", " ").trim();
        return collapsed.length() <= 44 ? collapsed : collapsed.substring(0, 41) + "...";
    }

    private void collectUnresolvedMarkers(String sourceText, List<String> unresolved) {
        if (sourceText == null || sourceText.isBlank()) {
            return;
        }

        Matcher unresolvedMatcher = UNRESOLVED_NOTE_PATTERN.matcher(sourceText);
        while (unresolvedMatcher.find()) {
            unresolved.add(unresolvedMatcher.group(1));
        }
    }

    private Map<String, Integer> collectUnresolvedByCategory(List<GroovyTraceView> traceViews) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        traceViews.stream()
                .flatMap(trace -> trace.unresolvedMarkers().stream())
                .forEach(marker -> counts.put(marker, counts.getOrDefault(marker, 0) + 1));
        return counts;
    }

    private String formatOriginLabel(GroovyTraceOriginKind originKind) {
        if (originKind == null) {
            return "unknown";
        }

        return switch (originKind) {
            case DIRECT_CONSTRUCTOR -> "direct";
            case FACADE_CALL -> "facade";
        };
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
                                   String sourceBindingName,
                                   GroovyTraceOriginKind originKind,
                                   String sourceExpressionText,
                                   String sagaClassFqn,
                                   String traceText,
                                   List<GroovyTraceArgument> constructorArguments,
                                   List<GroovyWorkflowCall> workflowCalls,
                                   List<String> resolutionNotes,
                                   List<String> unresolvedMarkers,
                                   List<UnresolvedCategoryView> unresolvedCategories,
                                   String traceCardId,
                                   String miniGraphId) {
    }

    private record UnresolvedCategoryView(String rawMarker,
                                          String normalizedLabel,
                                          String severityLevel,
                                          String severityClass,
                                          int occurrences) {
    }
}

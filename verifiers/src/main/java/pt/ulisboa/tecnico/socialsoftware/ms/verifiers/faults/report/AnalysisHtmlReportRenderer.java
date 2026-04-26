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
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyRuntimeCallRecipe;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueRecipe;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueMetadata;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueResolutionCategory;
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
                    .table-wrap {
                      width: 100%;
                      overflow-x: auto;
                      margin: 8px 0 0;
                    }
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
                      overflow-wrap: anywhere;
                      word-break: break-word;
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
                      overflow-wrap: anywhere;
                      word-break: break-word;
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
                    .trace-actions {
                      display: flex;
                      flex-wrap: wrap;
                      gap: 8px;
                      margin: 0 0 10px;
                    }
                    .severity-high { background: #ffe4e4; color: #8d2020; }
                    .severity-medium { background: #fff2de; color: #8a5300; }
                    .severity-low { background: #e8f2ff; color: #1b4f86; }
                    .trace-browser {
                      margin-top: 12px;
                    }
                    .trace-browser-controls {
                      position: sticky;
                      top: 10px;
                      z-index: 4;
                      background: rgba(255, 255, 255, 0.96);
                      border: 1px solid var(--line);
                      border-radius: 10px;
                      padding: 10px 12px;
                      box-shadow: 0 4px 16px rgba(17, 32, 38, 0.08);
                      margin-bottom: 12px;
                    }
                    .trace-browser-title {
                      margin: 0 0 8px;
                      font-size: 1rem;
                    }
                    .trace-browser-toolbar {
                      display: flex;
                      flex-wrap: wrap;
                      gap: 10px;
                      align-items: center;
                    }
                    .trace-view-switcher {
                      display: flex;
                      gap: 8px;
                      flex-wrap: wrap;
                    }
                    .trace-toolbar-actions {
                      display: flex;
                      gap: 8px;
                      flex-wrap: wrap;
                    }
                    .trace-toggle {
                      border: 1px solid var(--line);
                      background: #f6fbfc;
                      color: var(--text);
                      border-radius: 999px;
                      padding: 6px 12px;
                      cursor: pointer;
                      font: inherit;
                    }
                    .trace-toggle.active {
                      background: var(--accent);
                      color: #ffffff;
                      border-color: var(--accent);
                    }
                    .trace-filter,
                    .trace-sort {
                      border: 1px solid var(--line);
                      border-radius: 8px;
                      padding: 6px 9px;
                      font: inherit;
                      color: var(--text);
                      background: #ffffff;
                    }
                    .trace-filter {
                      min-width: 250px;
                      flex: 1 1 250px;
                    }
                    .trace-filter-status {
                      margin: 8px 0 0;
                      color: var(--muted);
                      font-size: 0.85rem;
                    }
                    .trace-group-view[hidden] {
                      display: none;
                    }
                    .trace-group,
                    .trace-cluster {
                      margin-bottom: 10px;
                    }
                    .trace-cluster > summary code,
                    .trace-group > summary code {
                      overflow-wrap: anywhere;
                    }
                    .trace-flow-card {
                      border: 1px solid var(--line);
                      border-radius: 10px;
                      background: linear-gradient(180deg, #fbfeff 0%, #f5fafb 100%);
                      padding: 10px 12px;
                      margin: 8px 0 10px;
                    }
                    .trace-flow-grid {
                      display: grid;
                      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
                      gap: 8px 12px;
                    }
                    .trace-flow-item {
                      min-width: 0;
                    }
                    .trace-flow-label {
                      display: block;
                      font-size: 0.73rem;
                      text-transform: uppercase;
                      letter-spacing: 0.04em;
                      color: var(--muted);
                      margin-bottom: 3px;
                    }
                    .trace-flow-value {
                      margin: 0;
                    }
                    .trace-instance {
                      scroll-margin-top: 84px;
                    }
                    .trace-instance-collection {
                      display: grid;
                      gap: 10px;
                    }
                    .trace-empty {
                      margin: 8px 0 0;
                      color: var(--muted);
                      font-style: italic;
                    }
                    .legacy-trace-hook {
                      display: none;
                    }
                  </style>
                </head>
                <body>
                <main>
                """);

        html.append("<section class=\"top\" id=\"top\">\n");
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
            html.append("<div class=\"table-wrap\"><table><thead><tr><th>Source class</th><th>Method</th><th>Binding</th><th>Saga constructor</th></tr></thead><tbody>\n");
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
            html.append("</tbody></table></div>\n");
        }
        html.append("</div></details>\n");

        appendReplayCategorySummary(html, fullTraceViews);

        html.append("<details class=\"subsection\"><summary>Detailed to deeper: unresolved input markers (")
                .append(unresolvedByCategory.values().stream().mapToInt(Integer::intValue).sum())
                .append(")</summary><div class=\"subsection-content\">\n");
        if (unresolvedByCategory.isEmpty()) {
            html.append("<p class=\"no-data\">No unresolved markers found in Groovy full traces.</p>\n");
        } else {
            html.append("<div class=\"table-wrap\"><table><thead><tr><th>Marker category</th><th>Count</th></tr></thead><tbody>\n");
            unresolvedByCategory.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                            .thenComparing(Map.Entry.comparingByKey()))
                    .forEach(entry -> html.append("<tr><td><span class=\"chip warn\">")
                            .append(escapeHtml(entry.getKey()))
                            .append("</span></td><td>")
                            .append(entry.getValue())
                            .append("</td></tr>\n"));
            html.append("</tbody></table></div>\n");
        }
        html.append("</div></details>\n");

        appendTraceBrowser(html, fullTraceViews);

        html.append("</div></details>\n");
    }

    private void appendTraceBrowser(StringBuilder html, List<GroovyTraceView> fullTraceViews) {
        List<TraceGroupView> sagaGroups = buildTraceGroupViews(fullTraceViews, "saga");
        List<TraceGroupView> sourceGroups = buildTraceGroupViews(fullTraceViews, "source");

        html.append("<section class=\"trace-browser\" id=\"groovy-trace-browser\" data-role=\"groovy-trace-browser\" data-default-view=\"saga\" data-active-view=\"saga\">\n");
        html.append("<div class=\"trace-browser-controls\" data-role=\"trace-browser-controls\">\n")
                .append("<h4 class=\"trace-browser-title\">Compare traces by saga target or source method</h4>\n")
                .append("<div class=\"trace-browser-toolbar\">\n")
                .append("<div class=\"trace-view-switcher\">\n")
                .append("<button type=\"button\" class=\"trace-toggle active\" data-role=\"trace-view-toggle\" data-view=\"saga\">By saga target</button>\n")
                .append("<button type=\"button\" class=\"trace-toggle\" data-role=\"trace-view-toggle\" data-view=\"source\">By source method</button>\n")
                .append("</div>\n")
                .append("<div class=\"trace-toolbar-actions\">\n")
                .append("<button type=\"button\" class=\"trace-toggle\" data-role=\"trace-expand-all\">Expand all</button>\n")
                .append("<button type=\"button\" class=\"trace-toggle\" data-role=\"trace-collapse-all\">Collapse all</button>\n")
                .append("</div>\n")
                .append("<input class=\"trace-filter\" type=\"search\" data-role=\"trace-filter\" placeholder=\"Filter saga, source method, binding, expression…\" />\n")
                .append("<select class=\"trace-sort\" data-role=\"trace-sort\">\n")
                .append("<option value=\"label\">Sort: label</option>\n")
                .append("<option value=\"traceCount\">Sort: trace count</option>\n")
                .append("<option value=\"unresolvedCount\">Sort: unresolved count</option>\n")
                .append("</select>\n")
                .append("</div>\n")
                .append("<p class=\"trace-filter-status\" data-role=\"trace-filter-status\">Showing ")
                .append(fullTraceViews.size())
                .append(fullTraceViews.size() == 1 ? " trace" : " traces")
                .append(" in the saga-target view.</p>\n")
                .append("</div>\n");

        appendTraceGroupView(html, "saga", sagaGroups, false);
        appendTraceGroupView(html, "source", sourceGroups, true);
        appendTraceBrowserData(html, sagaGroups, sourceGroups, fullTraceViews);
        appendTraceBrowserScript(html);
        html.append("</section>\n");
    }

    private List<TraceGroupView> buildTraceGroupViews(List<GroovyTraceView> fullTraceViews, String viewKind) {
        Map<String, List<GroovyTraceView>> grouped = fullTraceViews.stream()
                .collect(Collectors.groupingBy(
                        trace -> "source".equals(viewKind) ? formatSourceMethodLabel(trace) : simpleName(trace.sagaClassFqn()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<TraceGroupView> groups = new ArrayList<>();
        List<String> labels = grouped.keySet().stream().sorted().toList();
        int groupCounter = 1;
        int clusterCounter = 1;

        for (String label : labels) {
            Map<String, List<GroovyTraceView>> clusterMap = grouped.get(label).stream()
                    .collect(Collectors.groupingBy(
                            trace -> formatSourceMethodLabel(trace) + "→" + simpleName(trace.sagaClassFqn()),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));
            List<TraceClusterView> clusters = new ArrayList<>();
            for (String clusterKey : clusterMap.keySet().stream().sorted().toList()) {
                List<GroovyTraceView> traces = clusterMap.get(clusterKey).stream()
                        .sorted(Comparator.comparing((GroovyTraceView trace) -> trace.sourceBindingName() == null ? "" : trace.sourceBindingName())
                                .thenComparing(trace -> trace.sourceExpressionText() == null ? "" : trace.sourceExpressionText())
                                .thenComparing(GroovyTraceView::traceCardId))
                        .toList();
                GroovyTraceView anchor = traces.getFirst();
                clusters.add(new TraceClusterView(
                        "cluster-" + clusterCounter++,
                        formatSourceMethodLabel(anchor),
                        simpleName(anchor.sagaClassFqn()),
                        traces,
                        traces.size(),
                        traces.stream().mapToInt(trace -> trace.unresolvedMarkers().size()).sum()
                ));
            }
            clusters.sort(Comparator.comparing(TraceClusterView::sourceMethodLabel).thenComparing(TraceClusterView::sagaLabel));
            groups.add(new TraceGroupView(
                    "group-" + groupCounter++,
                    viewKind,
                    label,
                    clusters,
                    clusters.stream().mapToInt(TraceClusterView::instanceCount).sum(),
                    clusters.stream().mapToInt(TraceClusterView::unresolvedCount).sum()
            ));
        }

        return groups;
    }

    private void appendTraceGroupView(StringBuilder html,
                                      String viewKind,
                                      List<TraceGroupView> groups,
                                      boolean hidden) {
        html.append("<section class=\"trace-group-view\" data-role=\"trace-group-view\" data-view=\"")
                .append(viewKind)
                .append("\"");
        if (hidden) {
            html.append(" hidden");
        }
        html.append(">\n");
        if (groups.isEmpty()) {
            html.append("<p class=\"trace-empty\">No traces available for this view.</p>\n</section>\n");
            return;
        }
        groups.forEach(group -> appendTraceGroup(html, group));
        html.append("<p class=\"trace-empty\" data-role=\"trace-empty\" hidden>No matches for the current filter.</p>\n</section>\n");
    }

    private void appendTraceGroup(StringBuilder html, TraceGroupView group) {
        html.append("<details class=\"subsection trace-group\" data-role=\"trace-group\" data-group-kind=\"")
                .append(escapeHtml(group.viewKind()))
                .append("\" data-group-id=\"")
                .append(escapeHtml(group.groupId()))
                .append("\" data-group-label=\"")
                .append(escapeHtml(group.label()))
                .append("\" data-trace-count=\"")
                .append(group.traceCount())
                .append("\" data-unresolved-count=\"")
                .append(group.unresolvedCount())
                .append("\" id=\"")
                .append(escapeHtml(group.groupId()))
                .append("\" open><summary><code>")
                .append(escapeHtml(group.label()))
                .append("</code> <span class=\"chip\">trace(s): ")
                .append(group.traceCount())
                .append("</span> <span class=\"chip warn\">unresolved: ")
                .append(group.unresolvedCount())
                .append("</span></summary><div class=\"subsection-content\">\n");
        group.clusters().forEach(cluster -> appendTraceCluster(html, group, cluster));
        html.append("</div></details>\n");
    }

    private void appendTraceCluster(StringBuilder html, TraceGroupView group, TraceClusterView cluster) {
        html.append("<details class=\"subsection trace-cluster\" data-role=\"trace-cluster\" data-cluster-id=\"")
                .append(escapeHtml(cluster.clusterId()))
                .append("\" data-cluster-source-label=\"")
                .append(escapeHtml(cluster.sourceMethodLabel()))
                .append("\" data-cluster-saga-label=\"")
                .append(escapeHtml(cluster.sagaLabel()))
                .append("\" data-instance-count=\"")
                .append(cluster.instanceCount())
                .append("\" data-unresolved-count=\"")
                .append(cluster.unresolvedCount())
                .append("\" id=\"")
                .append(escapeHtml(cluster.clusterId()))
                .append("\"><summary><code>")
                .append(escapeHtml(cluster.sourceMethodLabel()))
                .append("</code> &rarr; <code>")
                .append(escapeHtml(cluster.sagaLabel()))
                .append("</code> <span class=\"chip\">")
                .append(cluster.instanceCount())
                .append(cluster.instanceCount() == 1 ? " instance" : " instance(s)")
                .append("</span> <span class=\"chip warn\">unresolved: ")
                .append(cluster.unresolvedCount())
                .append("</span></summary><div class=\"subsection-content\">\n");

        if ("saga".equals(group.viewKind())) {
            html.append("<div class=\"trace-instance-collection\">\n");
            cluster.traces().forEach(trace -> appendGroovyTraceInstance(html, trace, group));
            html.append("</div>\n");
        } else {
            html.append("<ul class=\"clean\">\n");
            cluster.traces().forEach(trace -> html.append("<li><a data-role=\"trace-jump\" href=\"#")
                    .append(escapeHtml(trace.traceCardId()))
                    .append("\"><code>")
                    .append(escapeHtml(trace.traceCardId()))
                    .append("</code></a> &mdash; <code>")
                    .append(escapeHtml(trace.sourceBindingName() == null || trace.sourceBindingName().isBlank() ? "(unknown binding)" : trace.sourceBindingName()))
                    .append("</code></li>\n"));
            html.append("</ul>\n");
        }

        html.append("</div></details>\n");
    }

    private void appendGroovyTraceInstance(StringBuilder html, GroovyTraceView trace, TraceGroupView group) {
        int argumentCount = constructorArgumentCount(trace.constructorArguments());

        html.append("<article class=\"trace-card trace-instance\" data-role=\"trace-instance\" data-trace-id=\"")
                .append(escapeHtml(trace.traceCardId()))
                .append("\" data-group-id=\"")
                .append(escapeHtml(group.groupId()))
                .append("\" id=\"")
                .append(escapeHtml(trace.traceCardId()))
                .append("\">\n");

        html.append("<p class=\"trace-actions\"><a href=\"#")
                .append(escapeHtml(group.groupId()))
                .append("\">Back to ")
                .append(escapeHtml("source".equals(group.viewKind()) ? "source group" : "saga group"))
                .append("</a> <a href=\"#groovy-trace-browser\">Back to browser</a> <a href=\"#top\">Back to top</a></p>\n");

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

        if (!trace.replayCategories().isEmpty()) {
            html.append("<p class=\"trace-chip-row\">\n");
            trace.replayCategories().forEach(category -> html.append("<span class=\"chip ")
                    .append(escapeHtml(category.severityClass()))
                    .append("\">replay: ")
                    .append(escapeHtml(category.normalizedLabel()))
                    .append(category.occurrences() > 1 ? " x" + category.occurrences() : "")
                    .append("</span>\n"));
            html.append("</p>\n");
        }

        appendTraceFlowCard(html, trace);
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

        html.append("<details class=\"subsection trace-raw\" data-role=\"trace-instance-raw\"><summary>Raw trace block</summary><div class=\"subsection-content\">\n")
                .append("<pre class=\"trace-pre\">")
                .append(escapeHtml(trace.traceText()))
                .append("</pre>\n")
                .append("</div></details>\n");

        html.append("</article>\n");
    }

    private void appendTraceFlowCard(StringBuilder html, GroovyTraceView trace) {
        html.append("<section class=\"trace-flow-card\" data-role=\"trace-flow-card\">\n<div class=\"trace-flow-grid\">\n");
        appendTraceFlowItem(html, "Source method", formatSourceMethodLabel(trace), true);
        appendTraceFlowItem(html, "Source expression",
                trace.sourceExpressionText() == null || trace.sourceExpressionText().isBlank()
                        ? "(none)"
                        : trace.sourceExpressionText(), true);
        appendTraceFlowItem(html, "Binding",
                trace.sourceBindingName() == null || trace.sourceBindingName().isBlank()
                        ? "(unknown)"
                        : trace.sourceBindingName(), true);
        appendTraceFlowItem(html, "Origin", formatOriginLabel(trace.originKind()), false);
        appendTraceFlowItem(html, "Target saga", simpleName(trace.sagaClassFqn()), true);
        html.append("</div>\n</section>\n");
    }

    private void appendTraceFlowItem(StringBuilder html, String label, String value, boolean codeStyle) {
        html.append("<div class=\"trace-flow-item\"><span class=\"trace-flow-label\">")
                .append(escapeHtml(label))
                .append("</span><p class=\"trace-flow-value\">");
        if (codeStyle) {
            html.append("<code>").append(escapeHtml(value)).append("</code>");
        } else {
            html.append(escapeHtml(value));
        }
        html.append("</p></div>\n");
    }

    private void appendTraceArgumentsTable(StringBuilder html, List<GroovyTraceArgument> constructorArguments) {
        html.append("<div class=\"table-wrap\"><table data-role=\"trace-arguments\"><thead><tr><th>Arg</th><th>Provenance</th><th>Replay</th><th>Value recipe</th></tr></thead><tbody>\n");
        if (constructorArguments.isEmpty()) {
            html.append("<tr><td colspan=\"4\" class=\"muted\">No constructor arguments were traced.</td></tr>\n");
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
                        appendReplayClassification(html, argument);
                        html.append("</td><td>");
                        appendValueRecipe(html, argument.recipe());
                        html.append("</td></tr>\n");
                    });
        }
        html.append("</tbody></table></div>\n");
    }

    private void appendReplayClassification(StringBuilder html, GroovyTraceArgument argument) {
        GroovyValueRecipe recipe = argument == null ? null : argument.recipe();
        GroovyValueMetadata metadata = recipe == null ? null : recipe.metadata();
        if (metadata == null) {
            html.append("<span class=\"muted\">(none)</span>");
            return;
        }

        GroovyValueResolutionCategory category = metadata.category();
        if (category != null && category != GroovyValueResolutionCategory.RESOLVED) {
            ReplayCategoryView replayCategory = toReplayCategoryView(category, 1);
            appendChip(html, "replay: " + replayCategory.normalizedLabel(), replayCategory.severityClass());
            html.append(" ");
        }

        String expectedTypeFqn = argument == null ? null : argument.expectedTypeFqn();
        if (expectedTypeFqn != null && !expectedTypeFqn.isBlank()) {
            appendChip(html, "type: " + expectedTypeFqn, null);
            html.append(" ");
        }

        GroovyRuntimeCallRecipe runtimeCall = metadata.runtimeCall();
        if (runtimeCall != null) {
            appendChip(html, "call: " + formatRuntimeCallLabel(runtimeCall), null);
        }

    }

    private void appendChip(StringBuilder html, String text, String severityClass) {
        html.append("<span class=\"chip");
        if (severityClass != null && !severityClass.isBlank()) {
            html.append(" ").append(escapeHtml(severityClass));
        }
        html.append("\">")
                .append(escapeHtml(text))
                .append("</span>");
    }

    private String formatRuntimeCallLabel(GroovyRuntimeCallRecipe runtimeCall) {
        if (runtimeCall == null) {
            return "(unknown)";
        }

        String methodName = runtimeCall.methodName();
        if (methodName == null || methodName.isBlank()) {
            return "(unknown)";
        }

        String receiverText = runtimeCall.receiverText();
        if (receiverText == null || receiverText.isBlank()) {
            return methodName + "()";
        }

        return receiverText + "." + methodName + "()";
    }

    private void appendReplayCategorySummary(StringBuilder html, List<GroovyTraceView> traceViews) {
        Map<GroovyValueResolutionCategory, Integer> counts = new LinkedHashMap<>();
        traceViews.stream()
                .flatMap(trace -> trace.replayCategories().stream())
                .forEach(category -> counts.put(category.category(), counts.getOrDefault(category.category(), 0) + category.occurrences()));

        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        html.append("<details class=\"subsection\"><summary>Replay-oriented unresolved categories (")
                .append(total)
                .append(")</summary><div class=\"subsection-content\">\n");
        if (counts.isEmpty()) {
            html.append("<p class=\"no-data\">No replay-oriented categories found in Groovy full traces.</p>\n");
        } else {
            html.append("<div class=\"table-wrap\"><table><thead><tr><th>Replay category</th><th>Severity</th><th>Count</th></tr></thead><tbody>\n");
            counts.entrySet().stream()
                    .map(entry -> toReplayCategoryView(entry.getKey(), entry.getValue()))
                    .sorted(Comparator.comparingInt(ReplayCategoryView::severityRank).reversed()
                            .thenComparing(ReplayCategoryView::normalizedLabel))
                    .forEach(category -> html.append("<tr><td>")
                            .append("<span class=\"chip ")
                            .append(escapeHtml(category.severityClass()))
                            .append("\">replay: ")
                            .append(escapeHtml(category.normalizedLabel()))
                            .append("</span></td><td><span class=\"chip ")
                            .append(escapeHtml(category.severityClass()))
                            .append("\">")
                            .append(escapeHtml(category.severityLevel()))
                            .append("</span></td><td>")
                            .append(category.occurrences())
                            .append("</td></tr>\n"));
            html.append("</tbody></table></div>\n");
        }
        html.append("</div></details>\n");
    }

    private void appendTraceBrowserData(StringBuilder html,
                                        List<TraceGroupView> sagaGroups,
                                        List<TraceGroupView> sourceGroups,
                                        List<GroovyTraceView> traces) {
        html.append("<script id=\"groovy-trace-browser-data\" type=\"application/json\">");
        html.append("{\"defaultView\":\"saga\",\"views\":[");
        appendTraceGroupJson(html, "saga", sagaGroups);
        html.append(",");
        appendTraceGroupJson(html, "source", sourceGroups);
        html.append("],\"traces\":[");
        for (int i = 0; i < traces.size(); i++) {
            GroovyTraceView trace = traces.get(i);
            if (i > 0) {
                html.append(",");
            }
            html.append("{\"traceId\":\"")
                    .append(escapeJsonString(trace.traceCardId()))
                    .append("\",\"sourceMethodLabel\":\"")
                    .append(escapeJsonString(formatSourceMethodLabel(trace)))
                    .append("\",\"sagaLabel\":\"")
                    .append(escapeJsonString(simpleName(trace.sagaClassFqn())))
                    .append("\",\"sourceExpressionText\":\"")
                    .append(escapeJsonString(trace.sourceExpressionText() == null ? "" : trace.sourceExpressionText()))
                    .append("\"}");
        }
        html.append("]}</script>\n");
    }

    private void appendTraceGroupJson(StringBuilder html, String viewKind, List<TraceGroupView> groups) {
        html.append("{\"view\":\"")
                .append(escapeJsonString(viewKind))
                .append("\",\"groups\":[");
        for (int i = 0; i < groups.size(); i++) {
            TraceGroupView group = groups.get(i);
            if (i > 0) {
                html.append(",");
            }
            html.append("{\"groupId\":\"")
                    .append(escapeJsonString(group.groupId()))
                    .append("\",\"label\":\"")
                    .append(escapeJsonString(group.label()))
                    .append("\",\"traceCount\":")
                    .append(group.traceCount())
                    .append(",\"unresolvedCount\":")
                    .append(group.unresolvedCount())
                    .append(",\"clusters\":[");
            for (int j = 0; j < group.clusters().size(); j++) {
                TraceClusterView cluster = group.clusters().get(j);
                if (j > 0) {
                    html.append(",");
                }
                html.append("{\"clusterId\":\"")
                        .append(escapeJsonString(cluster.clusterId()))
                        .append("\",\"sourceMethodLabel\":\"")
                        .append(escapeJsonString(cluster.sourceMethodLabel()))
                        .append("\",\"sagaLabel\":\"")
                        .append(escapeJsonString(cluster.sagaLabel()))
                        .append("\",\"instanceCount\":")
                        .append(cluster.instanceCount())
                        .append("}");
            }
            html.append("]}");
        }
        html.append("]}");
    }

    private void appendTraceBrowserScript(StringBuilder html) {
        html.append("<script>\n")
                .append("(() => {\n")
                .append("  const browser = document.querySelector('[data-role=\\\"groovy-trace-browser\\\"]');\n")
                .append("  if (!browser) return;\n")
                .append("  const views = Array.from(browser.querySelectorAll('[data-role=\\\"trace-group-view\\\"]'));\n")
                .append("  const toggles = Array.from(browser.querySelectorAll('[data-role=\\\"trace-view-toggle\\\"]'));\n")
                .append("  const expandAllButton = browser.querySelector('[data-role=\\\"trace-expand-all\\\"]');\n")
                .append("  const collapseAllButton = browser.querySelector('[data-role=\\\"trace-collapse-all\\\"]');\n")
                .append("  const filterInput = browser.querySelector('[data-role=\\\"trace-filter\\\"]');\n")
                .append("  const sortSelect = browser.querySelector('[data-role=\\\"trace-sort\\\"]');\n")
                .append("  const status = browser.querySelector('[data-role=\\\"trace-filter-status\\\"]');\n")
                .append("  const dataScript = document.getElementById('groovy-trace-browser-data');\n")
                .append("  let parsedData = { traces: [] };\n")
                .append("  try { parsedData = JSON.parse(dataScript.textContent || '{}'); } catch (error) { parsedData = { traces: [] }; }\n")
                .append("\n")
                .append("  const activeView = () => browser.dataset.activeView || browser.dataset.defaultView || 'saga';\n")
                .append("\n")
                .append("  const updateStatus = () => {\n")
                .append("    const current = views.find(view => !view.hasAttribute('hidden'));\n")
                .append("    if (!current || !status) return;\n")
                .append("    const visibleGroups = current.querySelectorAll('[data-role=\\\"trace-group\\\"]:not([hidden])').length;\n")
                .append("    const query = (filterInput?.value || '').trim();\n")
                .append("    const traceCount = parsedData.traces?.length || 0;\n")
                .append("    status.textContent = query\n")
                .append("      ? `Showing ${visibleGroups} group(s) for “${query}” in the ${activeView()} view (${traceCount} trace(s) total).`\n")
                .append("      : `Showing ${traceCount} trace(s) in the ${activeView()} view.`;\n")
                .append("  };\n")
                .append("\n")
                .append("  const sortGroups = (view) => {\n")
                .append("    const groups = Array.from(view.querySelectorAll('[data-role=\\\"trace-group\\\"]'));\n")
                .append("    const sortKey = sortSelect?.value || 'label';\n")
                .append("    groups.sort((left, right) => {\n")
                .append("      if (sortKey === 'traceCount') {\n")
                .append("        return Number(right.dataset.traceCount || 0) - Number(left.dataset.traceCount || 0)\n")
                .append("          || (left.dataset.groupLabel || '').localeCompare(right.dataset.groupLabel || '');\n")
                .append("      }\n")
                .append("      if (sortKey === 'unresolvedCount') {\n")
                .append("        return Number(right.dataset.unresolvedCount || 0) - Number(left.dataset.unresolvedCount || 0)\n")
                .append("          || (left.dataset.groupLabel || '').localeCompare(right.dataset.groupLabel || '');\n")
                .append("      }\n")
                .append("      return (left.dataset.groupLabel || '').localeCompare(right.dataset.groupLabel || '');\n")
                .append("    });\n")
                .append("    groups.forEach(group => view.appendChild(group));\n")
                .append("  };\n")
                .append("\n")
                .append("  const applyFilter = () => {\n")
                .append("    const current = views.find(view => !view.hasAttribute('hidden'));\n")
                .append("    if (!current) return;\n")
                .append("    const query = (filterInput?.value || '').trim().toLowerCase();\n")
                .append("    const groups = Array.from(current.querySelectorAll('[data-role=\\\"trace-group\\\"]'));\n")
                .append("    groups.forEach(group => {\n")
                .append("      let visibleClusters = 0;\n")
                .append("      group.querySelectorAll('[data-role=\\\"trace-cluster\\\"]').forEach(cluster => {\n")
                .append("        const haystack = `${cluster.dataset.clusterSourceLabel || ''} ${cluster.dataset.clusterSagaLabel || ''} ${cluster.textContent || ''}`.toLowerCase();\n")
                .append("        const matches = !query || haystack.includes(query);\n")
                .append("        cluster.hidden = !matches;\n")
                .append("        if (matches) visibleClusters += 1;\n")
                .append("      });\n")
                .append("      group.hidden = visibleClusters === 0;\n")
                .append("    });\n")
                .append("    const empty = current.querySelector('[data-role=\\\"trace-empty\\\"]');\n")
                .append("    if (empty) {\n")
                .append("      const anyVisible = groups.some(group => !group.hidden);\n")
                .append("      empty.hidden = anyVisible;\n")
                .append("    }\n")
                .append("    updateStatus();\n")
                .append("  };\n")
                .append("\n")
                .append("  const setAllDetails = (open) => {\n")
                .append("    const current = views.find(view => !view.hasAttribute('hidden'));\n")
                .append("    if (!current) return;\n")
                .append("    current.querySelectorAll('details').forEach(detail => { detail.open = open; });\n")
                .append("  };\n")
                .append("\n")
                .append("  const setView = (viewName) => {\n")
                .append("    browser.dataset.activeView = viewName;\n")
                .append("    views.forEach(view => {\n")
                .append("      view.hidden = view.dataset.view !== viewName;\n")
                .append("      if (!view.hidden) sortGroups(view);\n")
                .append("    });\n")
                .append("    toggles.forEach(toggle => toggle.classList.toggle('active', toggle.dataset.view === viewName));\n")
                .append("    applyFilter();\n")
                .append("  };\n")
                .append("\n")
                .append("  toggles.forEach(toggle => toggle.addEventListener('click', () => setView(toggle.dataset.view || 'saga')));\n")
                .append("  expandAllButton?.addEventListener('click', () => setAllDetails(true));\n")
                .append("  collapseAllButton?.addEventListener('click', () => setAllDetails(false));\n")
                .append("  browser.querySelectorAll('[data-role=\\\"trace-jump\\\"]').forEach(link => {\n")
                .append("    link.addEventListener('click', () => { setView('saga'); });\n")
                .append("  });\n")
                .append("  filterInput?.addEventListener('input', applyFilter);\n")
                .append("  sortSelect?.addEventListener('change', () => { const current = views.find(view => !view.hidden); if (current) sortGroups(current); applyFilter(); });\n")
                .append("  setView(activeView());\n")
                .append("})();\n")
                .append("</script>\n");
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
        List<GroovyTraceView> views = new ArrayList<>();

        int index = 1;
        for (GroovyFullTraceResult traceResult : traceResults) {
            String traceId = "trace-" + index;
            String graphId = "graph-" + index;
            index += 1;
            views.add(toGroovyTraceView(traceResult, traceId, graphId));
        }

        return views;
    }

    private GroovyTraceView toGroovyTraceView(GroovyFullTraceResult traceResult, String traceId, String graphId) {
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

        List<ReplayCategoryView> replayCategories = collectReplayCategories(constructorArguments);

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
                replayCategories,
                traceId,
                graphId
        );
    }

    private List<ReplayCategoryView> collectReplayCategories(List<GroovyTraceArgument> constructorArguments) {
        Map<GroovyValueResolutionCategory, Integer> counts = new LinkedHashMap<>();
        constructorArguments.stream()
                .map(this::replayCategoryOf)
                .filter(category -> category != null && category != GroovyValueResolutionCategory.RESOLVED)
                .forEach(category -> counts.put(category, counts.getOrDefault(category, 0) + 1));

        return counts.entrySet().stream()
                .map(entry -> toReplayCategoryView(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(ReplayCategoryView::severityRank).reversed()
                        .thenComparing(ReplayCategoryView::normalizedLabel))
                .toList();
    }

    private GroovyValueResolutionCategory replayCategoryOf(GroovyTraceArgument argument) {
        if (argument == null) {
            return null;
        }

        GroovyValueRecipe recipe = argument.recipe();
        if (recipe == null) {
            return null;
        }

        GroovyValueMetadata metadata = recipe.metadata();
        return metadata == null ? null : metadata.category();
    }

    private ReplayCategoryView toReplayCategoryView(GroovyValueResolutionCategory category, int occurrences) {
        GroovyValueResolutionCategory safeCategory = category == null ? GroovyValueResolutionCategory.UNKNOWN_UNRESOLVED : category;
        return switch (safeCategory) {
            case RUNTIME_CALL -> new ReplayCategoryView(safeCategory, "runtime call", "high", "severity-high", 3, occurrences);
            case SOURCE_PLACEHOLDER -> new ReplayCategoryView(safeCategory, "source placeholder", "low", "severity-low", 1, occurrences);
            case INJECTABLE_PLACEHOLDER -> new ReplayCategoryView(safeCategory, "injectable placeholder", "low", "severity-low", 1, occurrences);
            case UNKNOWN_UNRESOLVED -> new ReplayCategoryView(safeCategory, "unknown unresolved", "medium", "severity-medium", 2, occurrences);
            case RESOLVED -> new ReplayCategoryView(safeCategory, "resolved", "low", "severity-low", 0, occurrences);
        };
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

    private static String escapeJsonString(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                case '<' -> escaped.append("\\u003c");
                case '>' -> escaped.append("\\u003e");
                case '&' -> escaped.append("\\u0026");
                default -> {
                    if (ch < 0x20 || ch == 0x2028 || ch == 0x2029) {
                        escaped.append(String.format("\\u%04x", (int) ch));
                    } else {
                        escaped.append(ch);
                    }
                }
            }
        }
        return escaped.toString();
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
                                    List<ReplayCategoryView> replayCategories,
                                    String traceCardId,
                                    String miniGraphId) {
    }

    private record TraceGroupView(String groupId,
                                  String viewKind,
                                  String label,
                                  List<TraceClusterView> clusters,
                                  int traceCount,
                                  int unresolvedCount) {
    }

    private record TraceClusterView(String clusterId,
                                    String sourceMethodLabel,
                                    String sagaLabel,
                                    List<GroovyTraceView> traces,
                                    int instanceCount,
                                    int unresolvedCount) {
    }

    private record ReplayCategoryView(GroovyValueResolutionCategory category,
                                      String normalizedLabel,
                                      String severityLevel,
                                      String severityClass,
                                      int severityRank,
                                      int occurrences) {
    }
}

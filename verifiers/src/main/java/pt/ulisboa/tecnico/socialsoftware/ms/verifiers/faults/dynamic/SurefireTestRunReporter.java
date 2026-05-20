package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SurefireTestRunReporter {
    public static final String TEST_RUN_FILE_NAME = "test-run.json";
    public static final String TEST_RUNS_DIRECTORY = "test-runs";
    private static final int TESTSUITE_HEADER_CHAR_LIMIT = 65_536;
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("([A-Za-z_:][-A-Za-z0-9_:.]*)\\s*=\\s*(['\"])(.*?)\\2");
    private static final Pattern SPOCK_FEATURE_PATTERN = Pattern.compile("(?m)^\\s*def\\s+['\"]");
    private static final Pattern JUNIT_TEST_PATTERN = Pattern.compile("(?m)^\\s*@Test\\b");
    private static final Pattern GROOVY_EXTENDS_PATTERN = Pattern.compile("(?m)^\\s*class\\s+\\w+\\s+extends\\s+");

    private final ObjectMapper objectMapper;

    public SurefireTestRunReporter() {
        this(new ObjectMapper());
    }

    public SurefireTestRunReporter(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null")
                .copy()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public Result write(Path surefireReportsDirectory,
                        Path evidenceDirectory,
                        List<String> selectedTestClassFqns,
                        boolean mavenTimedOut) throws IOException {
        return write(surefireReportsDirectory, null, evidenceDirectory, selectedTestClassFqns, mavenTimedOut);
    }

    public Result write(Path surefireReportsDirectory,
                        Path applicationPath,
                        Path evidenceDirectory,
                        List<String> selectedTestClassFqns,
                        boolean mavenTimedOut) throws IOException {
        Objects.requireNonNull(evidenceDirectory, "evidenceDirectory cannot be null");
        Files.createDirectories(evidenceDirectory);
        Path sidecarDirectory = evidenceDirectory.resolve(TEST_RUNS_DIRECTORY);
        Files.createDirectories(sidecarDirectory);

        Map<String, ReportSummary> reports = readReports(surefireReportsDirectory);
        List<TestRunRecord> records = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        for (String testClassFqn : normalizedSelectedClasses(selectedTestClassFqns)) {
            ReportSummary report = reports.get(testClassFqn);
            TestRunRecord record;
            if (report == null) {
                String status = missingReportStatus(surefireReportsDirectory, applicationPath, testClassFqn, mavenTimedOut);
                String warning = "NO_REPORT".equals(status) ? "No Surefire report found for selected test class " + testClassFqn : null;
                if (warning != null) {
                    warnings.add(warning);
                }
                record = new TestRunRecord(testClassFqn, status, null, 0, 0, 0, 0, warning);
            } else {
                record = report.toRecord(testClassFqn);
            }
            records.add(record);
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(sidecarDirectory.resolve(safeTestClassFileName(testClassFqn) + ".json").toFile(), record.toMap());
        }

        Result result = new Result(records, statusCounts(records), warnings);
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(evidenceDirectory.resolve(TEST_RUN_FILE_NAME).toFile(), result.toMap());
        return result;
    }

    private Map<String, ReportSummary> readReports(Path surefireReportsDirectory) throws IOException {
        Map<String, ReportSummary> reports = new LinkedHashMap<>();
        if (surefireReportsDirectory == null || !Files.isDirectory(surefireReportsDirectory)) {
            return reports;
        }
        try (var files = Files.list(surefireReportsDirectory)) {
            List<Path> reportFiles = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith("TEST-"))
                    .filter(path -> path.getFileName().toString().endsWith(".xml"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
            for (Path reportFile : reportFiles) {
                Optional<ReportSummary> report = parseReport(reportFile);
                report.ifPresent(summary -> reports.put(summary.testClassFqn(), summary));
            }
        }
        return reports;
    }

    private String missingReportStatus(Path surefireReportsDirectory, Path applicationPath, String testClassFqn, boolean mavenTimedOut) {
        if (mavenTimedOut) {
            return "TIMED_OUT";
        }
        return selectedClassHasNoRunnableTestMethods(surefireReportsDirectory, applicationPath, testClassFqn) ? "PASSED" : "NO_REPORT";
    }

    private boolean selectedClassHasNoRunnableTestMethods(Path surefireReportsDirectory, Path applicationPath, String testClassFqn) {
        applicationPath = applicationPath == null ? applicationPathFromReportsDirectory(surefireReportsDirectory) : applicationPath;
        if (applicationPath == null || isBlank(testClassFqn)) {
            return false;
        }
        String relativeClassPath = testClassFqn.replace('.', '/');
        for (String extension : List.of(".groovy", ".java")) {
            Path sourcePath = applicationPath.resolve("src/test/groovy").resolve(relativeClassPath + extension);
            if (!Files.isRegularFile(sourcePath)) {
                sourcePath = applicationPath.resolve("src/test/java").resolve(relativeClassPath + extension);
            }
            if (Files.isRegularFile(sourcePath)) {
                try {
                    String source = Files.readString(sourcePath, StandardCharsets.UTF_8);
                    if (extension.equals(".groovy") && !GROOVY_EXTENDS_PATTERN.matcher(source).find()
                            && !JUNIT_TEST_PATTERN.matcher(source).find()) {
                        return true;
                    }
                    return !SPOCK_FEATURE_PATTERN.matcher(source).find()
                            && !JUNIT_TEST_PATTERN.matcher(source).find();
                } catch (IOException ignored) {
                    return false;
                }
            }
        }
        return false;
    }

    private Path applicationPathFromReportsDirectory(Path surefireReportsDirectory) {
        if (surefireReportsDirectory == null || surefireReportsDirectory.getParent() == null) {
            return null;
        }
        Path targetDirectory = surefireReportsDirectory.getParent();
        return targetDirectory == null ? null : targetDirectory.getParent();
    }

    private Optional<ReportSummary> parseReport(Path reportFile) throws IOException {
        try {
            Map<String, String> attributes = testsuiteAttributes(reportFile);
            String testClassFqn = firstText(attributes.get("classname"), attributes.get("name"));
            if (isBlank(testClassFqn)) {
                testClassFqn = classNameFromFile(reportFile);
            }
            return Optional.of(new ReportSummary(
                    testClassFqn,
                    reportFile.toString(),
                    intAttribute(attributes, "tests"),
                    intAttribute(attributes, "failures"),
                    intAttribute(attributes, "errors"),
                    intAttribute(attributes, "skipped")));
        } catch (Exception e) {
            throw new IOException("Failed to parse Surefire report " + reportFile + ": " + e.getMessage(), e);
        }
    }

    private Map<String, String> testsuiteAttributes(Path reportFile) throws IOException {
        StringBuilder header = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(reportFile, StandardCharsets.UTF_8)) {
            int next;
            while ((next = reader.read()) != -1 && header.length() < TESTSUITE_HEADER_CHAR_LIMIT) {
                header.append((char) next);
                int start = header.indexOf("<testsuite");
                if (start >= 0) {
                    int end = header.indexOf(">", start);
                    if (end > start) {
                        return attributes(header.substring(start, end + 1));
                    }
                }
            }
        }
        throw new IOException("Missing <testsuite> root header in first " + TESTSUITE_HEADER_CHAR_LIMIT + " characters");
    }

    private Map<String, String> attributes(String testsuiteHeader) {
        Map<String, String> attributes = new LinkedHashMap<>();
        Matcher matcher = ATTRIBUTE_PATTERN.matcher(testsuiteHeader);
        while (matcher.find()) {
            attributes.put(matcher.group(1), matcher.group(3));
        }
        return attributes;
    }

    private static String classNameFromFile(Path reportFile) {
        String fileName = reportFile.getFileName().toString();
        return fileName.substring("TEST-".length(), fileName.length() - ".xml".length());
    }

    private static int intAttribute(Map<String, String> attributes, String name) {
        String value = attributes.get(name);
        if (isBlank(value)) {
            return 0;
        }
        return Integer.parseInt(value);
    }

    private static String firstText(String first, String second) {
        return isBlank(first) ? normalize(second) : normalize(first);
    }

    private static List<String> normalizedSelectedClasses(List<String> selectedTestClassFqns) {
        return (selectedTestClassFqns == null ? List.<String>of() : selectedTestClassFqns).stream()
                .map(SurefireTestRunReporter::normalize)
                .filter(value -> !isBlank(value))
                .distinct()
                .sorted()
                .toList();
    }

    private static Map<String, Integer> statusCounts(List<TestRunRecord> records) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("passed", 0);
        counts.put("failed", 0);
        counts.put("timedOut", 0);
        counts.put("skipped", 0);
        counts.put("noReport", 0);
        for (TestRunRecord record : records) {
            switch (record.status()) {
                case "PASSED" -> counts.compute("passed", (key, value) -> value + 1);
                case "FAILED" -> counts.compute("failed", (key, value) -> value + 1);
                case "TIMED_OUT" -> counts.compute("timedOut", (key, value) -> value + 1);
                case "SKIPPED" -> counts.compute("skipped", (key, value) -> value + 1);
                case "NO_REPORT" -> counts.compute("noReport", (key, value) -> value + 1);
                default -> { }
            }
        }
        return counts;
    }

    private static String safeTestClassFileName(String testClassFqn) {
        String sanitized = (testClassFqn == null ? "test-class" : testClassFqn)
                .replaceAll("[^A-Za-z0-9._-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");
        return sanitized.isBlank() ? "test-class" : sanitized;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record Result(List<TestRunRecord> records, Map<String, Integer> statusCounts, List<String> warnings) {
        public Result {
            records = records == null ? List.of() : List.copyOf(records);
            statusCounts = statusCounts == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(statusCounts));
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("testRuns", records.stream().map(TestRunRecord::toMap).toList());
            map.put("statusCounts", statusCounts);
            map.put("warnings", warnings);
            return map;
        }
    }

    public record TestRunRecord(String testClassFqn,
                                String status,
                                String reportPath,
                                int tests,
                                int failures,
                                int errors,
                                int skipped,
                                String warning) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("testClassFqn", testClassFqn);
            map.put("status", status);
            map.put("reportPath", reportPath);
            map.put("tests", tests);
            map.put("failures", failures);
            map.put("errors", errors);
            map.put("skipped", skipped);
            if (warning != null) {
                map.put("warning", warning);
            }
            return map;
        }
    }

    private record ReportSummary(String testClassFqn,
                                 String reportPath,
                                 int tests,
                                 int failures,
                                 int errors,
                                 int skipped) {
        private TestRunRecord toRecord(String selectedTestClassFqn) {
            String status;
            if (failures > 0 || errors > 0) {
                status = "FAILED";
            } else if (tests > 0 && skipped == tests) {
                status = "SKIPPED";
            } else {
                status = "PASSED";
            }
            return new TestRunRecord(selectedTestClassFqn, status, reportPath, tests, failures, errors, skipped, null);
        }
    }
}

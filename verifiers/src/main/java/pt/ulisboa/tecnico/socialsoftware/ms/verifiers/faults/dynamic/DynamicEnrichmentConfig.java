package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic;

import java.util.List;
import java.util.Objects;

public record DynamicEnrichmentConfig(
        boolean enabled,
        boolean allowPartialTestRun,
        String dynamicEvidenceSubdir,
        String sidecarPath,
        String sidecarManifestPath,
        String joinReportPath,
        String testSourceRoot,
        List<String> includeTestDirs,
        List<String> excludeTestDirs,
        List<String> excludeTestClasses,
        int perTestTimeoutSeconds,
        DynamicEnrichmentMavenConfig maven
) {

    public DynamicEnrichmentConfig {
        dynamicEvidenceSubdir = requireText(dynamicEvidenceSubdir, "dynamicEvidenceSubdir");
        sidecarPath = requireText(sidecarPath, "sidecarPath");
        sidecarManifestPath = requireText(sidecarManifestPath, "sidecarManifestPath");
        joinReportPath = requireText(joinReportPath, "joinReportPath");
        testSourceRoot = requireText(testSourceRoot, "testSourceRoot");
        includeTestDirs = normalizeList(includeTestDirs);
        excludeTestDirs = normalizeList(excludeTestDirs);
        excludeTestClasses = normalizeList(excludeTestClasses);
        if (perTestTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("perTestTimeoutSeconds must be positive");
        }
        maven = maven == null ? new DynamicEnrichmentMavenConfig("mvn", "test-sagas") : maven;
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return value.trim();
    }

    private static List<String> normalizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    public record DynamicEnrichmentMavenConfig(String executable, String profile) {

        public DynamicEnrichmentMavenConfig {
            executable = requireText(executable, "maven.executable");
            profile = requireText(profile, "maven.profile");
        }
    }
}

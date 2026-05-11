package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class DynamicEnrichmentTestClassDiscoveryService {

    private static final Pattern PACKAGE_DECLARATION = Pattern.compile(
            "(?m)^\\s*package\\s+([A-Za-z_][\\w]*(?:\\s*\\.\\s*[A-Za-z_][\\w]*)*)\\s*;?\\s*$");

    public List<String> discover(Path applicationPath, DynamicEnrichmentConfig config) throws IOException {
        Objects.requireNonNull(applicationPath, "applicationPath cannot be null");
        Objects.requireNonNull(config, "config cannot be null");

        Path applicationRoot = applicationPath.toAbsolutePath().normalize();
        Path testRoot = resolveRelativeUnderRoot(applicationRoot, config.testSourceRoot(), "test source root");
        List<Path> includeDirs = resolveConfiguredDirectories(testRoot, config.includeTestDirs(), "include test dir");
        List<Path> excludeDirs = resolveConfiguredDirectories(testRoot, config.excludeTestDirs(), "exclude test dir");
        Set<String> excludedClasses = config.excludeTestClasses().stream().collect(Collectors.toSet());

        if (!Files.isDirectory(testRoot)) {
            return List.of();
        }

        TreeSet<String> discovered = new TreeSet<>();
        try (Stream<Path> files = Files.walk(testRoot)) {
            files.filter(file -> Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
                    .filter(file -> !Files.isSymbolicLink(file))
                    .filter(this::isSupportedTestSource)
                    .map(file -> toDiscoveredTestClass(testRoot, file))
                    .filter(candidate -> includeDirs.isEmpty() || matchesAny(candidate.file(), includeDirs))
                    .filter(candidate -> !matchesAny(candidate.file(), excludeDirs))
                    .filter(candidate -> !isExcluded(candidate, excludedClasses))
                    .map(DiscoveredTestClass::fqn)
                    .forEach(discovered::add);
        }

        return List.copyOf(discovered);
    }

    private List<Path> resolveConfiguredDirectories(Path root, List<String> configuredDirs, String label) {
        if (configuredDirs == null || configuredDirs.isEmpty()) {
            return List.of();
        }

        List<Path> normalized = new ArrayList<>();
        for (String configuredDir : configuredDirs) {
            if (configuredDir == null || configuredDir.isBlank()) {
                continue;
            }
            normalized.add(resolveRelativeUnderRoot(root, configuredDir, label));
        }
        return List.copyOf(normalized);
    }

    private static Path resolveRelativeUnderRoot(Path root, String configuredPath, String label) {
        Path configured = Path.of(configuredPath);
        if (configured.isAbsolute()) {
            throw new IllegalArgumentException(label + " must be relative to the application base dir: " + configuredPath);
        }

        Path resolved = root.resolve(configured).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException(label + " must stay under test source root: " + configuredPath);
        }

        return resolved;
    }

    private boolean matchesAny(Path candidate, List<Path> directories) {
        for (Path directory : directories) {
            if (candidate.startsWith(directory)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSupportedTestSource(Path file) {
        String fileName = file.getFileName().toString();
        return fileName.endsWith(".groovy") || fileName.endsWith(".java");
    }

    private DiscoveredTestClass toDiscoveredTestClass(Path testRoot, Path file) {
        String simpleClassName = stripExtension(file.getFileName().toString());
        String packageName = extractPackageName(file);
        String fqn = packageName == null ? deriveFqnFromPath(testRoot, file, simpleClassName) : packageName + "." + simpleClassName;
        return new DiscoveredTestClass(file.toAbsolutePath().normalize(), simpleClassName, fqn);
    }

    private String deriveFqnFromPath(Path testRoot, Path file, String simpleClassName) {
        Path relative = testRoot.relativize(file.toAbsolutePath().normalize());
        Path parent = relative.getParent();
        if (parent == null) {
            return simpleClassName;
        }

        String prefix = StreamSupport.stream(parent.spliterator(), false)
                .map(Path::toString)
                .collect(Collectors.joining("."));
        return prefix.isBlank() ? simpleClassName : prefix + "." + simpleClassName;
    }

    private String extractPackageName(Path file) {
        try {
            Matcher matcher = PACKAGE_DECLARATION.matcher(Files.readString(file));
            if (!matcher.find()) {
                return null;
            }

            return matcher.group(1)
                    .replaceAll("\\s*\\.\\s*", ".")
                    .trim();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read test source file: " + file, e);
        }
    }

    private boolean isExcluded(DiscoveredTestClass candidate, Set<String> excludedClasses) {
        return excludedClasses.contains(candidate.fqn()) || excludedClasses.contains(candidate.simpleClassName());
    }

    private String stripExtension(String fileName) {
        int extensionIdx = fileName.lastIndexOf('.');
        return extensionIdx < 0 ? fileName : fileName.substring(0, extensionIdx);
    }

    private record DiscoveredTestClass(Path file, String simpleClassName, String fqn) {
    }
}

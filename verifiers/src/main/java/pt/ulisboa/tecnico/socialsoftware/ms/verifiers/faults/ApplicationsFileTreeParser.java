package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults;

import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ApplicationsFileTreeParser {

    private final Map<String, Path> javaFilePaths = new HashMap<>();
    private final Map<String, Path> groovyFilePaths = new HashMap<>();

    public void parse(Path applicationsRoot) throws IOException {
        if (!Files.isDirectory(applicationsRoot)) {
            throw new IllegalArgumentException("Not a directory: " + applicationsRoot);
        }

        Files.walkFileTree(applicationsRoot, new SimpleFileVisitor<>() {
            @Override
            public @NonNull FileVisitResult visitFile(@NonNull Path file, @NonNull BasicFileAttributes attrs) {
                String fileName = file.getFileName().toString();
                String absolute = file.toAbsolutePath().toString();

                int mainJavaIdx = absolute.indexOf("/src/main/java/");
                int testGroovyIdx = absolute.indexOf("/src/test/groovy/");

                if (mainJavaIdx >= 0 && fileName.endsWith(".java")) {
                    String relative = absolute.substring(mainJavaIdx + "/src/main/java/".length());
                    String fqn = relative.replace("/", ".").replace(".java", "");
                    javaFilePaths.put(fqn, file);
                } else if (testGroovyIdx >= 0 && fileName.endsWith(".groovy")) {
                    String relative = absolute.substring(testGroovyIdx + "/src/test/groovy/".length());
                    String fqn = relative.replace("/", ".").replace(".groovy", "");
                    groovyFilePaths.put(fqn, file);
                }

                return FileVisitResult.CONTINUE;
            }
        });
    }

    public Map<String, Path> getJavaFilePaths() {
        return javaFilePaths;
    }

    public Map<String, Path> getJavaFilePathsForApplication(Path applicationsRoot, String applicationBaseDir) {
        Objects.requireNonNull(applicationsRoot, "applicationsRoot cannot be null");
        Objects.requireNonNull(applicationBaseDir, "applicationBaseDir cannot be null");

        Path applicationRoot = applicationsRoot.resolve(applicationBaseDir).toAbsolutePath().normalize();
        Map<String, Path> applicationJavaClasses = new HashMap<>();
        for (Map.Entry<String, Path> entry : javaFilePaths.entrySet()) {
            if (entry.getValue().toAbsolutePath().normalize().startsWith(applicationRoot)) {
                applicationJavaClasses.put(entry.getKey(), entry.getValue());
            }
        }
        return applicationJavaClasses;
    }

    public Map<String, Path> getGroovyFilePaths() {
        return groovyFilePaths;
    }
}

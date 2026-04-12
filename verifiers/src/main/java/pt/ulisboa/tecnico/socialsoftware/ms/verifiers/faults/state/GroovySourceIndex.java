package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state;

import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ImportNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.PackageNode;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.SourceUnit;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class GroovySourceIndex {

    private final Map<String, GroovySourceClassMetadata> classesByFqn = new LinkedHashMap<>();
    private final Map<String, String> sourceBackedSuperclassByClassFqn = new LinkedHashMap<>();

    public void parse(Path groovyTestRoot) throws IOException {
        Objects.requireNonNull(groovyTestRoot, "groovyTestRoot cannot be null");
        if (!Files.isDirectory(groovyTestRoot)) {
            throw new IllegalArgumentException("Not a directory: " + groovyTestRoot);
        }

        classesByFqn.clear();
        sourceBackedSuperclassByClassFqn.clear();

        Files.walkFileTree(groovyTestRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().endsWith(".groovy")) {
                    indexGroovySource(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        resolveSourceBackedSuperclasses();
    }

    public Map<String, GroovySourceClassMetadata> getClassesByFqn() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(classesByFqn));
    }

    public Map<String, String> getSourceBackedSuperclassByClassFqn() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(sourceBackedSuperclassByClassFqn));
    }

    public String getSourceBackedSuperclassFqn(String classFqn) {
        return sourceBackedSuperclassByClassFqn.get(classFqn);
    }

    private void indexGroovySource(Path file) throws IOException {
        String source = Files.readString(file);
        SourceUnit sourceUnit = SourceUnit.create(file.toString(), source);

        try {
            sourceUnit.parse();
            sourceUnit.completePhase();
            sourceUnit.convert();
        } catch (CompilationFailedException e) {
            throw new IllegalStateException("Failed to parse Groovy source: " + file, e);
        }

        ModuleNode moduleNode = sourceUnit.getAST();

        if (moduleNode == null) {
            return;
        }

        GroovySourceIndexVisitor visitor = new GroovySourceIndexVisitor(file, moduleNode);
        visitor.visitImports(moduleNode);
        moduleNode.getClasses().forEach(visitor::visitClass);
    }

    private void resolveSourceBackedSuperclasses() {
        for (Map.Entry<String, GroovySourceClassMetadata> entry : classesByFqn.entrySet()) {
            String sourceBackedSuperclassFqn = resolveSourceBackedSuperclass(entry.getValue());
            if (sourceBackedSuperclassFqn != null) {
                sourceBackedSuperclassByClassFqn.put(entry.getKey(), sourceBackedSuperclassFqn);
            }
        }
    }

    private String resolveSourceBackedSuperclass(GroovySourceClassMetadata metadata) {
        String declaredSuperclassName = metadata.declaredSuperclassName();
        if (declaredSuperclassName == null || declaredSuperclassName.isBlank()) {
            return null;
        }

        if ("Object".equals(declaredSuperclassName) || "java.lang.Object".equals(declaredSuperclassName)) {
            return null;
        }

        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(declaredSuperclassName);

        if (!declaredSuperclassName.contains(".") && metadata.packageName() != null && !metadata.packageName().isBlank()) {
            candidates.add(metadata.packageName() + "." + declaredSuperclassName);
        }

        for (GroovyImportMetadata importMetadata : metadata.imports()) {
            if (importMetadata.staticImport()) {
                continue;
            }

            if (importMetadata.star()) {
                if (importMetadata.importedType() != null && !importMetadata.importedType().isBlank()) {
                    candidates.add(importMetadata.importedType() + "." + declaredSuperclassName);
                }
                continue;
            }

            if (importMetadata.importedType() == null || importMetadata.importedType().isBlank()) {
                continue;
            }

            String importedSimpleName = simpleName(importMetadata.importedType());
            if (declaredSuperclassName.equals(importMetadata.alias())
                    || declaredSuperclassName.equals(importedSimpleName)
                    || declaredSuperclassName.equals(importMetadata.importedType())) {
                candidates.add(importMetadata.importedType());
            }
        }

        return candidates.stream()
                .filter(classesByFqn::containsKey)
                .findFirst()
                .orElse(null);
    }

    private static String simpleName(String fqn) {
        int lastDot = fqn.lastIndexOf('.');
        return lastDot < 0 ? fqn : fqn.substring(lastDot + 1);
    }

    private final class GroovySourceIndexVisitor extends ClassCodeVisitorSupport {

        private final Path sourceFile;
        private final ModuleNode moduleNode;
        private String packageName;
        private final List<GroovyImportMetadata> imports = new ArrayList<>();

        private GroovySourceIndexVisitor(Path sourceFile, ModuleNode moduleNode) {
            this.sourceFile = sourceFile;
            this.moduleNode = moduleNode;
            this.packageName = normalizePackageName(moduleNode.getPackageName());
        }

        @Override
        public void visitPackage(PackageNode packageNode) {
            packageName = packageNode == null ? null : normalizePackageName(packageNode.getName());
            super.visitPackage(packageNode);
        }

        @Override
        public void visitImports(ModuleNode ignored) {
            imports.clear();
            moduleNode.getImports().forEach(importNode -> imports.add(toImportMetadata(importNode)));
            moduleNode.getStarImports().forEach(importNode -> imports.add(toImportMetadata(importNode)));
            moduleNode.getStaticImports().values().forEach(importNode -> imports.add(toImportMetadata(importNode)));
            moduleNode.getStaticStarImports().values().forEach(importNode -> imports.add(toImportMetadata(importNode)));
            super.visitImports(ignored);
        }

        @Override
        public void visitClass(ClassNode node) {
            if (!node.isScript()) {
                String classFqn = node.getName();
                String declaredSuperclassName = declaredSuperclassName(node);
                classesByFqn.put(classFqn, new GroovySourceClassMetadata(
                        sourceFile,
                        packageName,
                        List.copyOf(imports),
                        declaredSuperclassName
                ));
            }

            super.visitClass(node);
        }

        @Override
        protected SourceUnit getSourceUnit() {
            return moduleNode.getContext();
        }

        private GroovyImportMetadata toImportMetadata(ImportNode importNode) {
            String importedType = importNode.isStar() ? importNode.getPackageName() : importNode.getClassName();
            return new GroovyImportMetadata(
                    importNode.getText(),
                    importedType,
                    importNode.getAlias(),
                    importNode.isStar(),
                    importNode.isStatic()
            );
        }

        private String declaredSuperclassName(ClassNode node) {
            ClassNode unresolvedSuperClass = node.getUnresolvedSuperClass();
            if (unresolvedSuperClass == null) {
                return null;
            }

            String unresolvedName = unresolvedSuperClass.getName();
            if (unresolvedName == null || unresolvedName.isBlank() || "java.lang.Object".equals(unresolvedName)) {
                return null;
            }

            return unresolvedName;
        }

        private String normalizePackageName(String packageName) {
            if (packageName == null) {
                return null;
            }

            String normalized = packageName.trim();
            while (normalized.endsWith(".")) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }

            return normalized.isBlank() ? null : normalized;
        }
    }
}

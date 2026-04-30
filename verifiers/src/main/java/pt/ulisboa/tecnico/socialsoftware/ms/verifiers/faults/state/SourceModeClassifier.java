package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SourceModeClassifier {
    private static final String SAGA_UOW_FQN =
            "pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWorkService";
    private static final String CAUSAL_UOW_FQN =
            "pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.unitOfWork.CausalUnitOfWorkService";

    private final Map<String, GroovySourceClassMetadata> classesByFqn;
    private final Map<String, String> sourceBackedSuperclassByClassFqn;

    public SourceModeClassifier() {
        this(Map.of(), Map.of());
    }

    public SourceModeClassifier(Map<String, GroovySourceClassMetadata> classesByFqn,
                                Map<String, String> sourceBackedSuperclassByClassFqn) {
        this.classesByFqn = classesByFqn == null ? Map.of() : new LinkedHashMap<>(classesByFqn);
        this.sourceBackedSuperclassByClassFqn = sourceBackedSuperclassByClassFqn == null
                ? Map.of()
                : new LinkedHashMap<>(sourceBackedSuperclassByClassFqn);
    }

    public SourceModeClassification classify(GroovySourceClassMetadata sourceClassMetadata) {
        if (sourceClassMetadata == null) {
            return SourceModeClassification.unknown();
        }

        List<Evidence> evidence = new ArrayList<>();
        collectActiveProfileEvidence(sourceClassMetadata, evidence);
        collectTestConfigurationEvidence(sourceClassMetadata, evidence);
        collectTypeEvidence(sourceClassMetadata, evidence);

        if (evidence.isEmpty()) {
            return SourceModeClassification.unknown();
        }

        int strongestRank = evidence.stream().mapToInt(Evidence::rank).max().orElse(-1);
        List<Evidence> strongest = evidence.stream()
                .filter(entry -> entry.rank() == strongestRank)
                .toList();

        boolean hasSagas = strongest.stream().anyMatch(entry -> entry.mode() == SourceMode.SAGAS);
        boolean hasTcc = strongest.stream().anyMatch(entry -> entry.mode() == SourceMode.TCC);
        List<String> descriptions = strongest.stream().map(Evidence::description).distinct().toList();

        if (hasSagas && hasTcc) {
            return new SourceModeClassification(SourceMode.MIXED, strongest.get(0).confidence(), descriptions, null);
        }

        SourceMode mode = hasSagas ? SourceMode.SAGAS : SourceMode.TCC;
        return new SourceModeClassification(mode, strongest.get(0).confidence(), descriptions, null);
    }

    private void collectActiveProfileEvidence(GroovySourceClassMetadata metadata, List<Evidence> evidence) {
        for (GroovySourceAnnotationMetadata annotation : metadata.annotations()) {
            if (annotationNameMatches(annotation, "ActiveProfiles")) {
                profileValues(annotation.attributes().get("value")).forEach(profile -> addProfileEvidence(profile, evidence,
                        "@ActiveProfiles(" + profile + ")"));
                profileValues(annotation.attributes().get("profiles")).forEach(profile -> addProfileEvidence(profile, evidence,
                        "@ActiveProfiles(profiles=" + profile + ")"));
            }

            if (annotationNameMatches(annotation, "TestPropertySource") || annotationNameMatches(annotation, "SpringBootTest")) {
                profileValues(annotation.attributes().get("properties")).forEach(property -> activeProfileFromProperty(property)
                        .forEach(profile -> addProfileEvidence(profile, evidence,
                                "@" + simpleName(annotation.name()) + "(" + property + ")")));
            }
        }
    }

    private void collectTestConfigurationEvidence(GroovySourceClassMetadata metadata, List<Evidence> evidence) {
        String sourceClassFqn = classFqn(metadata);
        if (sourceClassFqn == null) {
            return;
        }

        classesByFqn.values().stream()
                .filter(candidate -> sourceClassFqn.equals(candidate.enclosingClassFqn()))
                .filter(this::isTestConfigurationClass)
                .forEach(config -> collectBeanEvidenceFromConfig(config, evidence));
    }

    private void collectBeanEvidenceFromConfig(GroovySourceClassMetadata config,
                                               List<Evidence> evidence) {
        List<GroovySourceClassMetadata> chain = sourceBackedClassChain(config);
        for (GroovySourceClassMetadata configClass : chain) {
            for (GroovySourceMethodMetadata method : configClass.methods()) {
                if (!hasAnnotation(method.annotations(), "Bean")) {
                    continue;
                }

                SourceMode returnMode = modeForType(method.returnTypeName());
                if (returnMode != null) {
                    evidence.add(new Evidence(returnMode, SourceModeConfidence.TEST_CONFIGURATION, 3,
                            simpleName(classFqn(config)) + " -> @Bean " + method.name()
                                    + " returns " + method.returnTypeName()));
                }

                for (String constructedTypeName : method.constructedTypeNames()) {
                    SourceMode constructedMode = modeForType(constructedTypeName);
                    if (constructedMode != null) {
                        evidence.add(new Evidence(constructedMode, SourceModeConfidence.TEST_CONFIGURATION, 3,
                                simpleName(classFqn(config)) + " -> @Bean " + method.name()
                                        + " constructs " + constructedTypeName));
                    }
                }
            }
        }
    }

    private void collectTypeEvidence(GroovySourceClassMetadata metadata, List<Evidence> evidence) {
        collectFieldEvidence(metadata, evidence, 2, true);

        String nextSuperclass = sourceBackedSuperclassByClassFqn.get(classFqn(metadata));
        while (nextSuperclass != null) {
            GroovySourceClassMetadata superclassMetadata = classesByFqn.get(nextSuperclass);
            if (superclassMetadata == null) {
                return;
            }
            collectFieldEvidence(superclassMetadata, evidence, 1, false);
            nextSuperclass = sourceBackedSuperclassByClassFqn.get(nextSuperclass);
        }
    }

    private void collectFieldEvidence(GroovySourceClassMetadata metadata,
                                      List<Evidence> evidence,
                                      int rank,
                                      boolean local) {
        for (GroovySourceFieldMetadata field : metadata.fields()) {
            if (!hasAnnotation(field.annotations(), "Autowired")) {
                continue;
            }
            SourceMode mode = modeForType(field.typeName());
            if (mode == null) {
                continue;
            }
            int effectiveRank = rank;
            if (!local && autowiredRequiredFalse(field)) {
                effectiveRank = 0;
            }
            if (effectiveRank <= 0) {
                continue;
            }
            evidence.add(new Evidence(mode, SourceModeConfidence.TYPE_EVIDENCE, effectiveRank,
                    (local ? "@Autowired field " : "inherited @Autowired field ")
                            + field.typeName() + " " + field.name()));
        }
    }

    private List<GroovySourceClassMetadata> sourceBackedClassChain(GroovySourceClassMetadata first) {
        List<GroovySourceClassMetadata> chain = new ArrayList<>();
        GroovySourceClassMetadata current = first;
        while (current != null) {
            chain.add(current);
            String superclassFqn = sourceBackedSuperclassByClassFqn.get(classFqn(current));
            current = superclassFqn == null ? null : classesByFqn.get(superclassFqn);
        }
        return chain;
    }

    private boolean isTestConfigurationClass(GroovySourceClassMetadata metadata) {
        return hasAnnotation(metadata.annotations(), "TestConfiguration");
    }

    private boolean autowiredRequiredFalse(GroovySourceFieldMetadata field) {
        return field.annotations().stream()
                .filter(annotation -> annotationNameMatches(annotation, "Autowired"))
                .map(annotation -> annotation.attributes().get("required"))
                .anyMatch(Boolean.FALSE::equals);
    }

    private boolean hasAnnotation(List<GroovySourceAnnotationMetadata> annotations, String simpleName) {
        return annotations.stream().anyMatch(annotation -> annotationNameMatches(annotation, simpleName));
    }

    private boolean annotationNameMatches(GroovySourceAnnotationMetadata annotation, String expectedSimpleName) {
        return annotation != null && (expectedSimpleName.equals(annotation.name()) || expectedSimpleName.equals(simpleName(annotation.name())));
    }

    private List<String> profileValues(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return list.stream().filter(Objects::nonNull).map(Object::toString).toList();
        }
        return List.of(value.toString());
    }

    private List<String> activeProfileFromProperty(String property) {
        if (property == null) {
            return List.of();
        }
        String trimmed = property.trim();
        int separator = trimmed.indexOf('=');
        if (separator < 0) {
            return List.of();
        }
        String key = trimmed.substring(0, separator).trim();
        if (!"spring.profiles.active".equals(key)) {
            return List.of();
        }
        return java.util.Arrays.stream(trimmed.substring(separator + 1).trim().split(","))
                .map(String::trim)
                .filter(profile -> !profile.isBlank())
                .toList();
    }

    private void addProfileEvidence(String profile, List<Evidence> evidence, String description) {
        SourceMode mode = modeForProfile(profile);
        if (mode != null) {
            evidence.add(new Evidence(mode, SourceModeConfidence.ACTIVE_PROFILE, 3, description));
        }
    }

    private SourceMode modeForProfile(String profile) {
        if (profile == null) {
            return null;
        }
        return switch (profile.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "sagas" -> SourceMode.SAGAS;
            case "tcc" -> SourceMode.TCC;
            default -> null;
        };
    }

    private SourceMode modeForType(String typeName) {
        if (typeName == null) {
            return null;
        }
        if (SAGA_UOW_FQN.equals(typeName) || "SagaUnitOfWorkService".equals(typeName) || typeName.endsWith(".SagaUnitOfWorkService")) {
            return SourceMode.SAGAS;
        }
        if (CAUSAL_UOW_FQN.equals(typeName) || "CausalUnitOfWorkService".equals(typeName) || typeName.endsWith(".CausalUnitOfWorkService")) {
            return SourceMode.TCC;
        }
        return null;
    }

    private String classFqn(GroovySourceClassMetadata metadata) {
        return classesByFqn.entrySet().stream()
                .filter(entry -> entry.getValue() == metadata || entry.getValue().equals(metadata))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    private static String simpleName(String fqn) {
        if (fqn == null) {
            return null;
        }
        int lastDot = fqn.lastIndexOf('.');
        int lastDollar = fqn.lastIndexOf('$');
        int start = Math.max(lastDot, lastDollar);
        return start < 0 ? fqn : fqn.substring(start + 1);
    }

    private record Evidence(SourceMode mode,
                            SourceModeConfidence confidence,
                            int rank,
                            String description) {
    }
}

package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.EventDrivenArgumentSource;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.EventDrivenArgumentSourceKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.EventDrivenFunctionalityInvocation;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.WorkflowCreationArgumentSource;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.WorkflowCreationArgumentSourceKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.WorkflowFunctionalityCreationSite;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EventHandlingBridgeVisitor extends VoidVisitorAdapter<ApplicationAnalysisState> {
    private static final Logger logger = LoggerFactory.getLogger(EventHandlingBridgeVisitor.class);
    private static final Pattern JAVA_GETTER_PATTERN = Pattern.compile("([A-Za-z_$][A-Za-z\\d_$]*)\\.(?:get|is)([A-Z][A-Za-z\\d_$]*)\\(\\)");

    private final Map<String, ClassOrInterfaceDeclaration> classesByFqn = new LinkedHashMap<>();
    private final List<HandlingSubscription> subscriptions = new ArrayList<>();
    private final Set<String> subscriptionKeys = new LinkedHashSet<>();
    private final Set<String> emittedInvocationKeys = new LinkedHashSet<>();
    private final Set<String> emittedDiagnosticKeys = new LinkedHashSet<>();

    @Override
    public void visit(CompilationUnit cu, ApplicationAnalysisState state) {
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(decl -> {
            String fqn = classFqn(decl).orElse(null);
            if (fqn != null) {
                classesByFqn.putIfAbsent(fqn, decl);
            }
        });

        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(decl -> extractSubscriptions(decl));
        resolveKnownSubscriptions(state, false);
    }

    public void finish(ApplicationAnalysisState state) {
        resolveKnownSubscriptions(state, true);
    }

    private void extractSubscriptions(ClassOrInterfaceDeclaration eventHandlingClass) {
        String eventHandlingClassFqn = classFqn(eventHandlingClass).orElse(null);
        if (eventHandlingClassFqn == null) {
            return;
        }

        eventHandlingClass.getMethods().forEach(method -> method.findAll(MethodCallExpr.class).stream()
                .filter(call -> "handleSubscribedEvent".equals(call.getNameAsString()))
                .filter(call -> call.getArguments().size() >= 2)
                .forEach(call -> {
                    String eventTypeFqn = resolveEventType(call.getArgument(0)).orElse(null);
                    String eventHandlerClassFqn = resolveEventHandlerType(call.getArgument(1)).orElse(null);
                    if (eventTypeFqn == null || eventHandlerClassFqn == null) {
                        logger.debug("Could not resolve event subscription {}.{}: eventType={}, handlerType={}",
                                eventHandlingClassFqn, method.getNameAsString(), eventTypeFqn, eventHandlerClassFqn);
                        return;
                    }

                    HandlingSubscription subscription = new HandlingSubscription(
                            eventHandlingClassFqn,
                            method.getNameAsString(),
                            eventTypeFqn,
                            eventHandlerClassFqn);
                    if (subscriptionKeys.add(subscription.key())) {
                        subscriptions.add(subscription);
                    }
                }));
    }

    private Optional<String> resolveEventType(Expression expression) {
        if (expression instanceof ClassExpr classExpr) {
            return resolveType(classExpr.getType());
        }
        return Optional.empty();
    }

    private Optional<String> resolveEventHandlerType(Expression expression) {
        if (expression instanceof ObjectCreationExpr creationExpr) {
            return resolveType(creationExpr.getType());
        }
        return Optional.empty();
    }

    private Optional<String> resolveType(com.github.javaparser.ast.type.Type type) {
        try {
            return Optional.of(type.resolve().describe());
        } catch (Exception e) {
            logger.debug("Could not resolve type '{}': {}", type, e.getMessage());
            String text = type.asString();
            if (text.contains(".")) {
                return Optional.of(text);
            }
            return Optional.empty();
        }
    }

    private void resolveKnownSubscriptions(ApplicationAnalysisState state, boolean emitDiagnostics) {
        for (HandlingSubscription subscription : subscriptions) {
            ClassOrInterfaceDeclaration handlerClass = classesByFqn.get(subscription.eventHandlerClassFqn());
            if (handlerClass == null) {
                continue;
            }

            List<HandlerProcessingCall> processingCalls = resolveHandlerProcessingCalls(subscription, handlerClass);
            if (processingCalls.isEmpty()) {
                if (emitDiagnostics) {
                    addDiagnostic(state, subscription.key() + ": no resolvable event-processing call found in "
                            + subscription.eventHandlerClassFqn());
                }
                continue;
            }

            for (HandlerProcessingCall processingCall : processingCalls) {
                ClassOrInterfaceDeclaration processingClass = classesByFqn.get(processingCall.eventProcessingClassFqn());
                if (processingClass == null) {
                    continue;
                }

                List<EventDrivenFunctionalityInvocation> invocations = resolveProcessingFacadeInvocations(
                        subscription,
                        processingCall,
                        processingClass,
                        state);
                if (invocations.isEmpty()) {
                    if (emitDiagnostics) {
                        addDiagnostic(state, subscription.key() + ": no saga-backed facade call found in "
                                + processingCall.eventProcessingClassFqn() + "." + processingCall.eventProcessingMethodName() + "()");
                    }
                    continue;
                }

                for (EventDrivenFunctionalityInvocation invocation : invocations) {
                    addInvocation(state, invocation);
                }
            }
        }

        state.eventDrivenFunctionalityInvocations.sort(Comparator
                .comparing(EventDrivenFunctionalityInvocation::eventHandlingClassFqn)
                .thenComparing(EventDrivenFunctionalityInvocation::eventHandlingMethodName)
                .thenComparing(EventDrivenFunctionalityInvocation::sagaClassFqn));
    }

    private List<HandlerProcessingCall> resolveHandlerProcessingCalls(HandlingSubscription subscription,
                                                                      ClassOrInterfaceDeclaration handlerClass) {
        List<HandlerProcessingCall> processingCalls = new ArrayList<>();
        List<MethodDeclaration> handleMethods = handlerClass.getMethodsByName("handleEvent");
        for (MethodDeclaration handleMethod : handleMethods) {
            handleMethod.findAll(MethodCallExpr.class).forEach(call -> {
                Optional<String> receiverTypeFqn = resolveReceiverType(call.getScope().orElse(null), handlerClass);
                if (receiverTypeFqn.isEmpty() || !classesByFqn.containsKey(receiverTypeFqn.get())) {
                    return;
                }

                if (findMethods(receiverTypeFqn.get(), call.getNameAsString(), call.getArguments().size()).isEmpty()) {
                    return;
                }

                processingCalls.add(new HandlerProcessingCall(receiverTypeFqn.get(), call.getNameAsString()));
            });
        }

        return processingCalls.stream()
                .distinct()
                .sorted(Comparator.comparing(HandlerProcessingCall::eventProcessingClassFqn)
                        .thenComparing(HandlerProcessingCall::eventProcessingMethodName))
                .toList();
    }

    private List<EventDrivenFunctionalityInvocation> resolveProcessingFacadeInvocations(HandlingSubscription subscription,
                                                                                        HandlerProcessingCall processingCall,
                                                                                        ClassOrInterfaceDeclaration processingClass,
                                                                                        ApplicationAnalysisState state) {
        List<EventDrivenFunctionalityInvocation> invocations = new ArrayList<>();
        List<MethodDeclaration> processingMethods = findMethods(processingCall.eventProcessingClassFqn(),
                processingCall.eventProcessingMethodName(), null);
        for (MethodDeclaration processingMethod : processingMethods) {
            processingMethod.findAll(MethodCallExpr.class).forEach(facadeCall -> {
                Optional<String> facadeClassFqn = resolveReceiverType(facadeCall.getScope().orElse(null), processingClass);
                if (facadeClassFqn.isEmpty()) {
                    return;
                }

                List<WorkflowFunctionalityCreationSite> matchingSites = state.sagaCreationSites.stream()
                        .filter(site -> Objects.equals(site.classFqn(), facadeClassFqn.get()))
                        .filter(site -> Objects.equals(site.methodName(), facadeCall.getNameAsString()))
                        .toList();
                if (matchingSites.isEmpty()) {
                    return;
                }

                MethodDeclaration facadeMethod = findMethods(facadeClassFqn.get(),
                        facadeCall.getNameAsString(), facadeCall.getArguments().size()).stream()
                        .findFirst()
                        .orElse(null);

                for (WorkflowFunctionalityCreationSite creationSite : matchingSites) {
                    List<EventDrivenArgumentSource> argumentSources = buildArgumentSources(
                            subscription,
                            processingCall,
                            processingMethod,
                            facadeCall,
                            facadeMethod,
                            creationSite);
                    List<String> resolutionNotes = List.of(
                            "resolved via event handler " + simpleName(subscription.eventHandlingClassFqn()) + "." + subscription.eventHandlingMethodName() + "()",
                            "event type " + subscription.eventTypeFqn(),
                            "handler " + subscription.eventHandlerClassFqn(),
                            "processing " + processingCall.eventProcessingClassFqn() + "." + processingCall.eventProcessingMethodName() + "(...) ",
                            "facade " + facadeClassFqn.get() + "." + facadeCall.getNameAsString() + "(...) ",
                            "saga " + creationSite.sagaClassFqn());

                    invocations.add(new EventDrivenFunctionalityInvocation(
                            subscription.eventHandlingClassFqn(),
                            subscription.eventHandlingMethodName(),
                            subscription.eventTypeFqn(),
                            subscription.eventHandlerClassFqn(),
                            processingCall.eventProcessingClassFqn(),
                            processingCall.eventProcessingMethodName(),
                            facadeClassFqn.get(),
                            facadeCall.getNameAsString(),
                            creationSite.sagaClassFqn(),
                            argumentSources,
                            resolutionNotes));
                }
            });
        }

        return invocations;
    }

    private List<EventDrivenArgumentSource> buildArgumentSources(HandlingSubscription subscription,
                                                                 HandlerProcessingCall processingCall,
                                                                 MethodDeclaration processingMethod,
                                                                 MethodCallExpr facadeCall,
                                                                 MethodDeclaration facadeMethod,
                                                                 WorkflowFunctionalityCreationSite creationSite) {
        List<Expression> facadeCallArguments = facadeCall.getArguments();
        Set<String> facadeEventParameterNames = eventParameterNames(facadeMethod, subscription.eventTypeFqn());
        Map<String, Parameter> processingParametersByName = parametersByName(processingMethod);

        return creationSite.argumentSources().stream()
                .sorted(Comparator.comparingInt(WorkflowCreationArgumentSource::argumentIndex))
                .map(source -> buildArgumentSource(subscription,
                        processingCall,
                        facadeCallArguments,
                        facadeEventParameterNames,
                        processingParametersByName,
                        source))
                .toList();
    }

    private EventDrivenArgumentSource buildArgumentSource(HandlingSubscription subscription,
                                                          HandlerProcessingCall processingCall,
                                                          List<Expression> facadeCallArguments,
                                                          Set<String> facadeEventParameterNames,
                                                          Map<String, Parameter> processingParametersByName,
                                                          WorkflowCreationArgumentSource source) {
        if (source.kind() == WorkflowCreationArgumentSourceKind.FIELD_REFERENCE) {
            return new EventDrivenArgumentSource(source.argumentIndex(),
                    EventDrivenArgumentSourceKind.INJECTABLE_FIELD,
                    "field " + defaultText(source.name()) + " [event bridge injectable placeholder]",
                    source.name(),
                    placeholderId(subscription, source.argumentIndex(), source.name()),
                    null);
        }

        if (source.kind() == WorkflowCreationArgumentSourceKind.LOCAL_VARIABLE) {
            if (source.text() != null && source.text().contains("(")) {
                return new EventDrivenArgumentSource(source.argumentIndex(),
                        EventDrivenArgumentSourceKind.RUNTIME_CALL,
                        defaultText(source.name()) + " <- " + displayRuntimeCall(source.text()) + " [unresolved external/runtime edge]",
                        source.text(),
                        placeholderId(subscription, source.argumentIndex(), source.name()),
                        null);
            }

            return new EventDrivenArgumentSource(source.argumentIndex(),
                    EventDrivenArgumentSourceKind.SOURCE_PLACEHOLDER,
                    "local " + defaultText(source.name()) + " [event bridge source placeholder]",
                    source.name(),
                    placeholderId(subscription, source.argumentIndex(), source.name()),
                    null);
        }

        if (source.kind() == WorkflowCreationArgumentSourceKind.METHOD_PARAMETER) {
            Integer parameterIndex = source.parameterIndex();
            if (parameterIndex == null || parameterIndex < 0 || parameterIndex >= facadeCallArguments.size()) {
                return eventExpression(subscription, source, "EVENT_PARAMETER:unresolved-index-" + parameterIndex,
                        "event parameter index " + parameterIndex + " [unresolved event bridge parameter]");
            }

            return classifyFacadeCallArgument(subscription,
                    source,
                    facadeCallArguments.get(parameterIndex),
                    processingParametersByName);
        }

        if (source.kind() == WorkflowCreationArgumentSourceKind.INLINE_EXPRESSION) {
            return classifyInlineExpression(subscription, source, facadeEventParameterNames);
        }

        return eventExpression(subscription, source, defaultText(source.text()),
                defaultText(source.text()) + " [event bridge expression placeholder]");
    }

    private EventDrivenArgumentSource classifyFacadeCallArgument(HandlingSubscription subscription,
                                                                 WorkflowCreationArgumentSource source,
                                                                 Expression callArgument,
                                                                 Map<String, Parameter> processingParametersByName) {
        Optional<EventField> eventField = eventFieldFromExpression(callArgument, Set.of());
        if (eventField.isPresent()) {
            return eventFieldArgument(subscription, source, eventField.get().fieldName());
        }

        Expression unwrapped = unwrapCast(callArgument);
        if (unwrapped instanceof NameExpr nameExpr) {
            String parameterName = nameExpr.getNameAsString();
            Parameter processingParameter = processingParametersByName.get(parameterName);
            if (processingParameter != null) {
                String parameterTypeFqn = resolveType(processingParameter.getType()).orElse(null);
                if (Objects.equals(parameterTypeFqn, subscription.eventTypeFqn())) {
                    return new EventDrivenArgumentSource(source.argumentIndex(),
                            EventDrivenArgumentSourceKind.EVENT_PAYLOAD,
                            "EVENT_PAYLOAD:" + simpleName(subscription.eventTypeFqn()) + " via " + parameterName,
                            "EVENT_PAYLOAD:" + subscription.eventTypeFqn(),
                            placeholderId(subscription, source.argumentIndex(), "event"),
                            null);
                }

                if (looksLikeSubscriberAggregateId(parameterName, processingParameter)) {
                    return new EventDrivenArgumentSource(source.argumentIndex(),
                            EventDrivenArgumentSourceKind.EVENT_SUBSCRIBER_AGGREGATE_ID,
                            "EVENT_SUBSCRIBER_AGGREGATE_ID:" + parameterName,
                            "EVENT_SUBSCRIBER_AGGREGATE_ID",
                            placeholderId(subscription, source.argumentIndex(), parameterName),
                            null);
                }

                return new EventDrivenArgumentSource(source.argumentIndex(),
                        EventDrivenArgumentSourceKind.EVENT_PROCESSING_PARAMETER,
                        "EVENT_PROCESSING_PARAMETER:" + parameterName,
                        "EVENT_PROCESSING_PARAMETER:" + parameterName,
                        placeholderId(subscription, source.argumentIndex(), parameterName),
                        null);
            }
        }

        return eventExpression(subscription, source, callArgument.toString(),
                callArgument + " [event bridge expression placeholder]");
    }

    private EventDrivenArgumentSource classifyInlineExpression(HandlingSubscription subscription,
                                                               WorkflowCreationArgumentSource source,
                                                               Set<String> facadeEventParameterNames) {
        String text = defaultText(source.text());
        Optional<EventField> eventField = eventFieldFromText(text, facadeEventParameterNames);
        if (eventField.isPresent()) {
            return eventFieldArgument(subscription, source, eventField.get().fieldName());
        }

        return eventExpression(subscription, source, text,
                text + " [event bridge expression placeholder]");
    }

    private EventDrivenArgumentSource eventFieldArgument(HandlingSubscription subscription,
                                                         WorkflowCreationArgumentSource source,
                                                         String eventFieldName) {
        String fieldName = eventFieldName == null || eventFieldName.isBlank() ? "(unknown)" : eventFieldName;
        String recipeText = "EVENT_FIELD:" + simpleName(subscription.eventTypeFqn()) + "." + fieldName;
        return new EventDrivenArgumentSource(source.argumentIndex(),
                EventDrivenArgumentSourceKind.EVENT_FIELD,
                recipeText,
                recipeText,
                placeholderId(subscription, source.argumentIndex(), fieldName),
                fieldName);
    }

    private EventDrivenArgumentSource eventExpression(HandlingSubscription subscription,
                                                      WorkflowCreationArgumentSource source,
                                                      String recipeText,
                                                      String provenance) {
        return new EventDrivenArgumentSource(source.argumentIndex(),
                EventDrivenArgumentSourceKind.EVENT_EXPRESSION,
                provenance,
                recipeText,
                placeholderId(subscription, source.argumentIndex(), recipeText),
                null);
    }

    private Optional<EventField> eventFieldFromExpression(Expression expression, Set<String> allowedEventParameterNames) {
        Expression unwrapped = unwrapCast(expression);
        if (!(unwrapped instanceof MethodCallExpr methodCallExpr)) {
            return Optional.empty();
        }

        String methodName = methodCallExpr.getNameAsString();
        Optional<String> fieldName = eventFieldNameFromGetter(methodName);
        if (fieldName.isEmpty()) {
            return Optional.empty();
        }

        Optional<String> scopeName = methodCallExpr.getScope()
                .filter(Expression::isNameExpr)
                .map(Expression::asNameExpr)
                .map(NameExpr::getNameAsString);
        if (!allowedEventParameterNames.isEmpty()
                && scopeName.isPresent()
                && !allowedEventParameterNames.contains(scopeName.get())) {
            return Optional.empty();
        }

        return Optional.of(new EventField(fieldName.get()));
    }

    private Optional<EventField> eventFieldFromText(String text, Set<String> allowedEventParameterNames) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        Matcher matcher = JAVA_GETTER_PATTERN.matcher(text.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }

        String receiver = matcher.group(1);
        if (!allowedEventParameterNames.isEmpty() && !allowedEventParameterNames.contains(receiver)) {
            return Optional.empty();
        }

        return Optional.of(new EventField(decapitalize(matcher.group(2))));
    }

    private Optional<String> eventFieldNameFromGetter(String methodName) {
        if (methodName == null || methodName.isBlank()) {
            return Optional.empty();
        }
        if (methodName.startsWith("get") && methodName.length() > 3) {
            return Optional.of(decapitalize(methodName.substring(3)));
        }
        if (methodName.startsWith("is") && methodName.length() > 2) {
            return Optional.of(decapitalize(methodName.substring(2)));
        }
        return Optional.empty();
    }

    private Expression unwrapCast(Expression expression) {
        Expression current = expression;
        while (current instanceof CastExpr castExpr) {
            current = castExpr.getExpression();
        }
        return current;
    }

    private boolean looksLikeSubscriberAggregateId(String parameterName, Parameter parameter) {
        String lower = parameterName == null ? "" : parameterName.toLowerCase();
        if (lower.contains("subscriber") && lower.contains("aggregate")) {
            return true;
        }
        if (lower.equals("aggregateid") || lower.endsWith("aggregateid")) {
            try {
                return parameter.getType().resolve().describe().equals("java.lang.Integer")
                        || parameter.getType().resolve().describe().equals("int");
            } catch (Exception ignored) {
                return true;
            }
        }
        return false;
    }

    private Set<String> eventParameterNames(MethodDeclaration method, String eventTypeFqn) {
        if (method == null) {
            return Set.of();
        }

        return method.getParameters().stream()
                .filter(parameter -> resolveType(parameter.getType())
                        .map(typeFqn -> Objects.equals(typeFqn, eventTypeFqn))
                        .orElse(false))
                .map(Parameter::getNameAsString)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map<String, Parameter> parametersByName(MethodDeclaration method) {
        if (method == null) {
            return Map.of();
        }

        Map<String, Parameter> parameters = new LinkedHashMap<>();
        method.getParameters().forEach(parameter -> parameters.put(parameter.getNameAsString(), parameter));
        return parameters;
    }

    private Optional<String> resolveReceiverType(Expression scope, ClassOrInterfaceDeclaration ownerClass) {
        if (scope == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(scope.calculateResolvedType().describe());
        } catch (Exception e) {
            logger.debug("Could not resolve receiver expression '{}': {}", scope, e.getMessage());
        }

        String fieldName = fieldName(scope).orElse(null);
        if (fieldName != null) {
            return resolveVisibleFieldType(ownerClass, fieldName);
        }

        if (scope instanceof ObjectCreationExpr creationExpr) {
            return resolveType(creationExpr.getType());
        }

        return Optional.empty();
    }

    private Optional<String> fieldName(Expression expression) {
        if (expression instanceof NameExpr nameExpr) {
            return Optional.of(nameExpr.getNameAsString());
        }
        if (expression instanceof FieldAccessExpr fieldAccessExpr
                && fieldAccessExpr.getScope().isThisExpr()) {
            return Optional.of(fieldAccessExpr.getNameAsString());
        }
        return Optional.empty();
    }

    private Optional<String> resolveVisibleFieldType(ClassOrInterfaceDeclaration ownerClass, String fieldName) {
        ClassOrInterfaceDeclaration current = ownerClass;
        Set<String> visited = new LinkedHashSet<>();
        while (current != null) {
            String currentFqn = classFqn(current).orElse(current.getNameAsString());
            if (!visited.add(currentFqn)) {
                return Optional.empty();
            }

            for (FieldDeclaration field : current.getFields()) {
                for (VariableDeclarator variable : field.getVariables()) {
                    if (Objects.equals(variable.getNameAsString(), fieldName)) {
                        return resolveType(variable.getType());
                    }
                }
            }

            current = resolveSuperclass(current).orElse(null);
        }
        return Optional.empty();
    }

    private Optional<ClassOrInterfaceDeclaration> resolveSuperclass(ClassOrInterfaceDeclaration declaration) {
        for (ClassOrInterfaceType extendedType : declaration.getExtendedTypes()) {
            Optional<String> superclassFqn = resolveType(extendedType);
            if (superclassFqn.isPresent() && classesByFqn.containsKey(superclassFqn.get())) {
                return Optional.of(classesByFqn.get(superclassFqn.get()));
            }
        }
        return Optional.empty();
    }

    private List<MethodDeclaration> findMethods(String classFqn, String methodName, Integer arity) {
        ClassOrInterfaceDeclaration declaration = classesByFqn.get(classFqn);
        if (declaration == null || methodName == null) {
            return List.of();
        }

        return declaration.getMethodsByName(methodName).stream()
                .filter(method -> arity == null || method.getParameters().size() == arity)
                .sorted(Comparator.comparing(MethodDeclaration::getNameAsString)
                        .thenComparing(method -> method.getParameters().size()))
                .toList();
    }

    private Optional<String> classFqn(ClassOrInterfaceDeclaration declaration) {
        try {
            return Optional.of(declaration.resolve().getQualifiedName());
        } catch (Exception e) {
            return declaration.getFullyQualifiedName();
        }
    }

    private void addInvocation(ApplicationAnalysisState state, EventDrivenFunctionalityInvocation invocation) {
        String key = invocationKey(invocation);
        if (!emittedInvocationKeys.add(key)) {
            return;
        }
        boolean alreadyInState = state.eventDrivenFunctionalityInvocations.stream()
                .anyMatch(existing -> Objects.equals(invocationKey(existing), key));
        if (!alreadyInState) {
            state.eventDrivenFunctionalityInvocations.add(invocation);
            logger.info("Event-driven invocation: {}.{}() -> {} via {}.{}()",
                    invocation.eventHandlingClassFqn(),
                    invocation.eventHandlingMethodName(),
                    invocation.sagaClassFqn(),
                    invocation.facadeClassFqn(),
                    invocation.facadeMethodName());
        }
    }

    private String invocationKey(EventDrivenFunctionalityInvocation invocation) {
        return String.join("|",
                defaultText(invocation.eventHandlingClassFqn()),
                defaultText(invocation.eventHandlingMethodName()),
                defaultText(invocation.eventTypeFqn()),
                defaultText(invocation.eventHandlerClassFqn()),
                defaultText(invocation.eventProcessingClassFqn()),
                defaultText(invocation.eventProcessingMethodName()),
                defaultText(invocation.facadeClassFqn()),
                defaultText(invocation.facadeMethodName()),
                defaultText(invocation.sagaClassFqn()),
                invocation.argumentSources().stream()
                        .map(source -> source.argumentIndex() + ":" + source.kind() + ":" + defaultText(source.recipeText()))
                        .collect(Collectors.joining(",")));
    }

    private void addDiagnostic(ApplicationAnalysisState state, String diagnostic) {
        if (diagnostic == null || diagnostic.isBlank() || !emittedDiagnosticKeys.add(diagnostic)) {
            return;
        }
        state.eventDrivenFunctionalityDiagnostics.add(diagnostic);
        logger.debug("Event bridge diagnostic: {}", diagnostic);
    }

    private String placeholderId(HandlingSubscription subscription, int argumentIndex, String label) {
        return "event:"
                + subscription.eventHandlingClassFqn()
                + ":"
                + subscription.eventHandlingMethodName()
                + ":arg"
                + argumentIndex
                + ":"
                + defaultText(label);
    }

    private String displayRuntimeCall(String sourceText) {
        if (sourceText == null || sourceText.isBlank()) {
            return "(unknown)";
        }
        int openParen = sourceText.indexOf('(');
        if (openParen <= 0) {
            return sourceText;
        }
        String receiverAndMethod = sourceText.substring(0, openParen).trim();
        return receiverAndMethod + "(...)";
    }

    private String defaultText(String value) {
        return value == null || value.isBlank() ? "(unknown)" : value;
    }

    private String simpleName(String fqn) {
        if (fqn == null || fqn.isBlank()) {
            return "(unknown)";
        }
        int lastDot = fqn.lastIndexOf('.');
        return lastDot < 0 ? fqn : fqn.substring(lastDot + 1);
    }

    private String decapitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    private record HandlingSubscription(String eventHandlingClassFqn,
                                        String eventHandlingMethodName,
                                        String eventTypeFqn,
                                        String eventHandlerClassFqn) {
        private String key() {
            return eventHandlingClassFqn + "#" + eventHandlingMethodName + "#" + eventTypeFqn + "#" + eventHandlerClassFqn;
        }
    }

    private record HandlerProcessingCall(String eventProcessingClassFqn,
                                         String eventProcessingMethodName) {
    }

    private record EventField(String fieldName) {
    }
}

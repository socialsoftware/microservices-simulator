grammar Nebula

entry Model:
    (aggregates+=Aggregate | sharedEnums+=SharedEnums)*
    (exceptions=ExceptionMessages)?;

Aggregate:
    (annotations+=Annotation)*
    'Aggregate' name=ID '{'
        (generateCrud?='@GenerateCrud')?
        (aggregateElements+=AggregateElement)*
    '}';

// Aggregate elements - cardinality constraints enforced by validator:
// - Entity, Method, Workflow: Multiple allowed
// - Repository, Events, References, WebAPIEndpoints, ServiceDefinition, Functionalities: Only one allowed
AggregateElement:
    Entity | Method | Workflow | Repository | Events | References | WebAPIEndpoints | ServiceDefinition | Functionalities;

Entity:
    (isRoot?='Root')? (generateDto?='Dto')? 'Entity' name=ID
    ('from' aggregateRef=ID)?
    (
        '{'
            (properties+=Property | fieldMappings+=DtoFieldMapping)*
            ('invariants' '{'
                invariants+=Invariant*
            '}')?
        '}'
    )?;

// New syntax: map dtoField as entityField
// Example: map name as courseName
// Extract pattern: map collection.field as entityField
// Example: map questions.aggregateId as questionIds
DtoFieldMapping:
    'map' dtoField=DtoFieldPath 'as' entityField=ID;

// Support dotted paths for field extraction (e.g., questions.aggregateId)
DtoFieldPath:
    parts+=ID ('.' parts+=ID)*;

Method:
    (annotations+=Annotation)*
    'method' name=ID '(' (parameters+=Parameter (',' parameters+=Parameter)*)? ')'
    (':' returnType=ReturnType)?
    ('{' body=STRING '}')?;

Workflow:
    (annotations+=Annotation)*
    workflowType=WorkflowType 'workflow' name=ID '(' (parameters+=Parameter (',' parameters+=Parameter)*)? ')'
    (':' returnType=ReturnType)?
    (
        '{'
            ('fields' '{'
                workflowFields+=WorkflowField*
            '}')?
            ('steps' '{'
                workflowSteps+=WorkflowStep*
            '}')?
        '}'
    )?;

WorkflowField:
    type=ParamType name=ID;

// ============================================================================
// STEP PATTERNS (Consolidated Design)
// ============================================================================
// Both Workflow and Functionality/Saga steps follow the same structural pattern:
//
// Pattern Structure:
//   1. Step keyword ('step' for workflows, 'sync' for sagas)
//   2. Step name (ID)
//   3. Optional dependencies (shared StepDependencies rule)
//   4. Body with context-specific actions
//   5. Optional compensation block (mirrors step actions)
//
// Shared Components:
//   - StepDependencies: Common dependency syntax for both contexts
//
// Context-Specific Components:
//   - WorkflowStep uses WorkflowStepAction (supports dotted args, extract)
//   - SyncStep uses StepAction (supports result setting, simple args)
//
// This design maintains consistency while allowing context-specific semantics.
// ============================================================================

// WORKFLOW STEP PATTERN
// ============================================================================

WorkflowStep:
    'step' stepName=ID
    (dependsOn=StepDependencies)?
    '{'
        stepActions+=WorkflowStepAction*
    '}'
    (compensation=WorkflowStepCompensation)?;

WorkflowStepCompensation:
    'compensation' '{'
        compensationActions+=WorkflowStepAction*
    '}';

// Workflow step actions (context: Workflow orchestration)
WorkflowStepAction:
    WorkflowCallAction          // Call a service method
    | WorkflowExtractAction     // Extract data from collections
    | WorkflowRegisterStateAction  // Register saga state
    | WorkflowSetFieldAction;   // Set workflow field value

WorkflowCallAction:
    'call' serviceRef=ID '.' method=ID '(' (args+=WorkflowArg (',' args+=WorkflowArg)*)? ')'
    ('->' assignTo=ID)?;

WorkflowExtractAction:
    'extract' source=WorkflowArg '->' target=ID
    ('where' filterField=ID '==' filterValue=WorkflowArg)?;

WorkflowRegisterStateAction:
    'registerState' '(' aggregateId=WorkflowArg ',' sagaState=QualifiedName ')';

WorkflowSetFieldAction:
    'set' fieldName=ID '=' value=WorkflowArg;

// Workflow argument (supports dotted property access: user.name.first)
WorkflowArg:
    ref=ID ('.' chain+=ID)*;

WorkflowType returns string:
    'causal' | 'saga';

Repository:
    'Repository' '{'
        repositoryMethods+=RepositoryMethod*
    '}';

Events:
    'Events' '{'
        (publishedEvents+=PublishedEvent)*
        (subscribedEvents+=SubscribedEvent)*
        (interInvariants+=InterInvariant)*
    '}';

PublishedEvent:
    'publish' name=ID '{'
        fields+=EventField*
    '}';

SubscribedEvent:
    // Simple subscription: only reference the event name as a string.
    // We intentionally do NOT require a PublishedEvent definition here, since
    // many events (e.g., standard CRUD events) are generated automatically.
    'subscribe' eventType=ID
    ('from' sourceAggregate=ID)?
    ('routing' '(' routingIdExpr=Expression (',' routingVersionExpr=Expression)? ')')?
    (
        '{'
            conditions+=EventCondition*
        '}'
    )?;

EventField:
    type=Type name=ID;

EventCondition:
    condition=Expression?;

InterInvariant:
    'interInvariant' name=ID '{'
        (subscribedEvents+=SubscribedEvent)*
    '}';

References:
    'References' '{'
        (constraints+=ReferenceConstraint)*
    '}';

ReferenceConstraint:
    fieldName=ID '->' targetAggregate=ID '{'
        'onDelete' ':' action=ReferenceAction
        'message' ':' message=STRING
    '}';

ReferenceAction returns string:
    'prevent' | 'cascade' | 'setNull';

RepositoryMethod:
    ('@Query' '(' query=STRING ')')? returnType=RepositoryReturnType name=ID '(' (parameters+=Parameter (',' parameters+=Parameter)*)? ')';

RepositoryReturnType:
    ParamType;

Parameter:
    (annotations+=Annotation)*
    type=ParamType name=ID;

Annotation:
    '@' name=ID ('(' (values+=AnnotationValue (',' values+=AnnotationValue)*)? ')')?;

AnnotationValue:
    key=ID '=' value=(STRING | ID | LITERAL) | 
    value=(STRING | ID | LITERAL);

ParamType:
    PrimitiveType | EntityType | BuiltinType | ListType | SetType | OptionalType;

ReturnType:
    'void' | PrimitiveType | EntityType | BuiltinType | ListType | SetType | OptionalType;

Property:
    (isFinal?='final')? type=Type name=ID (',' names+=ID)* ('=' defaultValue=PropertyDefaultValue)? (dtoExclude?='dto-exclude')?;

PropertyDefaultValue returns string:
    LITERAL
  | STRING
  | QualifiedName
  | AggregateStateQualified
  | 'true'
  | 'false'
  | 'null'
  | '[]';

AggregateStateQualified returns string:
    'AggregateState' '.' ID;

Invariant:
    'check' name=ID '{'
        conditions+=InvariantCondition*
    '}'
    'error' errorMessage=STRING;

InvariantCondition:
    expression=Expression;

Expression:
    BooleanExpression;

BooleanExpression:
    {infer BooleanExpression} left=Comparison
    (op=("||"|"&&"|"OR"|"AND") right=Comparison)*;

Comparison:
    {infer Comparison} left=Addition
    (op=("<" | ">" | "<=" | ">=" | "==" | "!=") right=Addition)*;

Addition:
    {infer Addition} left=Multiplication
    (op=('+' | '-') right=Multiplication)*;

Multiplication:
    {infer Multiplication} left=Prefixed
    (op=("*"|"/"|"%") right=Prefixed)*;

Prefixed:
    NegationExpression |
    AtomicExpression;

NegationExpression:
    {infer NegationExpression}
    ("!" | "NOT") expression=AtomicExpression;

AtomicExpression:
    LiteralExpression |
    ParenthesizedExpression |
    MethodExpression |
    PropertyChainExpression |
    CollectionExpression;

LiteralExpression:
    {infer LiteralExpression}
    value=(LITERAL | 'true' | 'false' | 'null');

ParenthesizedExpression:
    {infer ParenthesizedExpression} 
    '(' expression=Expression ')';

PropertyChainExpression:
    {infer PropertyChainExpression}
    head=PropertyReference 
    (
        {infer MethodCall.receiver=current} '.' method=ID '(' (arguments+=Expression (',' arguments+=Expression)*)? ')' |
        {infer PropertyAccess.receiver=current} '.' member=ID
    )*;

PropertyReference:
    {infer PropertyReference} name=ID;

MethodExpression:
    UniqueCheckExpression |
    CollectionOperationExpression |
    TimeExpression |
    QuantifierExpression;

UniqueCheckExpression:
    {infer UniqueCheckExpression}
    collection=PropertyReference '.unique(' property=ID ')';

CollectionOperationExpression:
    {infer CollectionOperationExpression}
    collection=PropertyReference '.' operation=('size' | 'isEmpty' | 'count' | 'distinct' | 'findAny' | 'findFirst' | 'getState');

TimeExpression:
    {infer TimeExpression} 
    'now()' | 
    date=PropertyReference '.' operation=('isBefore' | 'isAfter' | 'isEqual') '(' argument=Expression ')';

QuantifierExpression:
    {infer QuantifierExpression} 
    quantifier=('forall' | 'exists') 
    variable=ID ':' 
    collection=PropertyReference '|' 
    body=Expression;

CollectionExpression:
    {infer CollectionExpression}
    collection=PropertyReference '.' collectionOperation=(
        'allMatch' | 'anyMatch' | 'noneMatch'
    ) '(' variable=ID '->' body=Expression ')';

// Pure primitive types (no void, no UnitOfWork)
PrimitiveType:
    typeName=('Integer' | 'Long' | 'Float' | 'Double' | 'String' | 'Boolean' | 'LocalDateTime' | 'Object');

// Special framework types
BuiltinType:
    typeName=('UnitOfWork' | 'AggregateState');

// Entity reference type
EntityType:
    type=[Entity:QualifiedName];

// Base type = any non-collection, non-void type
BaseType:
    PrimitiveType | BuiltinType | EntityType;

// Collection types with consistent element type
ListType:
    'List' '<' elementType=BaseType '>';

SetType:
    'Set' '<' elementType=BaseType '>';

OptionalType:
    'Optional' '<' elementType=BaseType '>';

// Complete type system
Type:
    BaseType | ListType | SetType | OptionalType;

QualifiedName returns string:
    ID ('.' ID)*;

terminal ID:
    /[_a-zA-Z][a-zA-Z0-9_]*/;

terminal LITERAL:
    /'[^']*'/ | /[0-9]+(\.[0-9]+)?/ | 'DELETED' | 'ANONYMOUS';

terminal STRING:
    /"[^"]*"/;

hidden terminal WS:
    /\s+/;

hidden terminal ML_COMMENT:
    /\/\*([^*]|\*+[^*/])*\*+\//;

hidden terminal SL_COMMENT:
    /\/\/[^\n\r]*/;

ExceptionMessages:
    'exceptions' '{'
        messages+=ExceptionMessage*
    '}';

ExceptionMessage:
    name=ID ':' message=STRING;

WebAPIEndpoints:
    'WebAPIEndpoints' '{'
        (endpoints+=CustomEndpoint)*
    '}';

// Endpoint syntax
CustomEndpoint:
    'Endpoint' name=ID '{'
        ('httpMethod' ':' method=HttpMethod)?
        ('path' ':' path=STRING)?
        ('methodName' ':' methodName=ID)?
        ('parameters' ':' '[' parameters+=EndpointParameter (',' parameters+=EndpointParameter)* ']')?
        ('returnType' ':' returnType=ParamType)?
        ('desc' ':' description=STRING)?
        ('throwsException' ':' throwsException=('true' | 'false'))?
    '}';

EndpointParameter:
    name=ID ':' type=ParamType ':' annotation=STRING;

HttpMethod:
    method=('GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH');

ServiceDefinition:
    'Service' (name=ID)? '{'
        (transactional?='@Transactional')?
        ('methods' '{'
            serviceMethods+=ServiceMethod*
        '}')?
    '}';

Functionalities:
    'Functionalities' '{'
        functionalityMethods+=FunctionalityMethod*
    '}';

FunctionalityMethod:
    name=ID '(' (parameters+=Parameter (',' parameters+=Parameter)*)? ')'
    (':' returnType=ReturnType)? ('throws' exceptionType=ID)?
    (
        'dependencies' '{' dependencyRefs+=ID (',' dependencyRefs+=ID)* '}'
    )?
    (
        'steps' '{'
            functionalitySteps+=FunctionalityStep*
        '}'
    )?;

// ============================================================================
// FUNCTIONALITY/SAGA STEP PATTERN
// Shared structure: stepName + dependencies + actions + compensation
// (Mirrors WorkflowStep pattern for consistency)
// ============================================================================

FunctionalityStep:
    SyncStep;

SyncStep:
    'sync' stepName=ID
    (dependsOn=StepDependencies)?
    '{'
        stepActions+=StepAction*
    '}'
    (compensation=StepCompensation)?;

// Shared step dependency structure (used by both Workflow and Saga steps)
StepDependencies:
    'depends' 'on' dependencies+=ID (',' dependencies+=ID)*;

StepCompensation:
    'compensation' '{'
        compensationActions+=StepAction*
    '}';

// Functionality/Saga step actions (context: Saga coordination)
StepAction:
    FuncCallServiceAction       // Call a service method
    | FuncSetResultAction       // Set result for functionality return
    | FuncAssignVariableAction  // Assign to local variable
    | FuncRegisterSagaStateAction;  // Register saga state

FuncCallServiceAction:
    'call' serviceRef=ID '.' method=ID '(' (args+=StepArgument (',' args+=StepArgument)*)? ')'
    ('assign' 'to' assignTo=ID)?;

FuncSetResultAction:
    'set' 'result' 'to' resultType=ID ('as' getterName=ID)?;

FuncAssignVariableAction:
    'set' variableName=ID '=' expression=StepExpression;

FuncRegisterSagaStateAction:
    'register' 'sagaState' aggregateId=StepArgument 'as' sagaState=ID 'in' unitOfWork=ID;

// Functionality step argument (simple: string literal or ID)
StepArgument:
    {infer StepArgument} (STRING | ID);

StepExpression:
    STRING | LITERAL | Expression | ID;

ServiceMethod:
    (annotations+=Annotation)*
    name=ID '(' (parameters+=Parameter (',' parameters+=Parameter)*)? ')'
    (':' returnType=ReturnType)?;

// Enum Support
EnumDefinition:
    'enum' name=ID '{'
        values+=EnumValue (',' values+=EnumValue)*
    '}';

EnumValue:
    name=ID;

// Shared Enums Support
SharedEnums:
    'SharedEnums' '{'
        enums+=EnumDefinition*
    '}';

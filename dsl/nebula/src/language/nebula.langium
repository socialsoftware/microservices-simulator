grammar Nebula

entry Model:
    imports+=Import*
    (aggregates+=Aggregate | sharedEnums+=SharedEnums)*
    (exceptions=ExceptionMessages)?;

Import:
    'import' sharedEnums?='shared-enums' ';';

Aggregate:
    (annotations+=Annotation)*
    (metadata=AggregateMetadata)?
    'Aggregate' name=ID '{'
        (aggregateElements+=AggregateElement)*
    '}';

AggregateElement:
    Entity | Method | Workflow | CustomRepository | Events | WebAPIEndpoints | ServiceDefinition | Functionalities;

Entity:
    (isRoot?='Root')? (generateDto?='Dto')? 'Entity' name=ID ('uses' 'dto' dtoType=ID (dtoMapping=DtoEntityMapping)?)? '{'
        properties+=Property*
        constructors+=Constructor*
        ('invariants' '{'
            invariants+=Invariant*
        '}')?
        ('methods' '{'
            methods+=Method*
        '}')?
    '}';

DtoEntityMapping:
    'mapping' '{'
        fieldMappings+=DtoFieldMapping*
    '}';

DtoFieldMapping:
    dtoField=ID '->' entityField=ID ('extract' extractField=ID)? ';';

Method:
    (annotations+=Annotation)*
    'method' name=ID '(' (parameters+=Parameter (',' parameters+=Parameter)*)? ')' 
    (':' returnType=ReturnType)? 
    ('{' body=STRING '}' | ';');

Constructor:
    'constructor' name=ID '(' (parameters+=Parameter (',' parameters+=Parameter)*)? ')' 
    ('{' body=STRING '}' | ';');

Workflow:
    (annotations+=Annotation)*
    workflowType=WorkflowType 'workflow' name=ID '(' (parameters+=Parameter (',' parameters+=Parameter)*)? ')' 
    (':' returnType=ReturnType)? ';';

WorkflowType returns string:
    'causal' | 'saga';

CustomRepository:
    'CustomRepository' '{'
        repositoryMethods+=RepositoryMethod*
    '}';

Events:
    'Events' '{'
        (publishedEvents+=PublishedEvent)*
        (subscribedEvents+=SubscribedEvent)*
    '}';

PublishedEvent:
    'publish' name=ID '{'
        fields+=EventField*
    '}';

SubscribedEvent:
    'subscribe' eventType=[PublishedEvent:QualifiedName] 'from' sourceAggregate=ID '{'
        conditions+=EventCondition*
    '}';

EventField:
    type=Type name=ID ';';

EventCondition:
    condition=STRING ';';

RepositoryMethod:
    ('@Query' '(' query=STRING ')')? returnType=RepositoryReturnType name=ID '(' (parameters+=Parameter (',' parameters+=Parameter)*)? ')' ';';

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

BuiltinType:
    name=('UnitOfWork' | 'AggregateState');

ReturnType:
    'void' | PrimitiveType | EntityType | BuiltinType | ListType | SetType | OptionalType;

ListType:
    'List' '<' elementType=(PrimitiveType | ID) '>';

SetType:
    'Set' '<' elementType=(PrimitiveType | ID) '>';

OptionalType:
    'Optional' '<' elementType=(PrimitiveType | ID) '>';

Property:
    (isFinal?='final')? type=Type name=ID (',' names+=ID)* ('default' defaultValue=PropertyDefaultValue)? (dtoExclude?='dto-exclude')? ';';

PropertyDefaultValue returns string:
    LITERAL | STRING | QualifiedName | 'null' | '[]';

Invariant:
    'check' name=ID '{'
        conditions+=InvariantCondition*
    '}';

InvariantCondition:
    expression=Expression ';';

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
    SignedExpression | 
    AtomicExpression;

NegationExpression:
    {infer NegationExpression} 
    ("!" | "NOT") expression=AtomicExpression;

SignedExpression:
    {infer SignedExpression} 
    '-' expression=AtomicExpression;

AtomicExpression:
    LiteralExpression |
    TernaryExpression |
    ParenthesizedExpression |
    MethodExpression |
    PropertyChainExpression |
    CollectionExpression;

LiteralExpression:
    {infer LiteralExpression} 
    value=(LITERAL | 'null');

TernaryExpression:
    {infer TernaryExpression}
    'if' condition=BooleanExpression
    'then' trueValue=AtomicExpression
    'else' falseValue=AtomicExpression;

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
        'allMatch' | 'anyMatch' | 'noneMatch' | 'filter' | 'map' | 'flatMap'
    ) '(' variable=ID '->' body=Expression ')';

Type:
    PrimitiveType | CollectionType | EntityType | AggregateStateType | OptionalType;

PrimitiveType:
    typeName=('Integer' | 'Long' | 'Float' | 'Double' | 'String' | 'Boolean' | 'LocalDateTime');

CollectionType:
    'Set' '<' elementType=(EntityType | PrimitiveType) '>' |
    'List' '<' elementType=(EntityType | PrimitiveType) '>';

EntityType:
    type=[Entity:QualifiedName];

AggregateStateType:
    typeName=('AggregateState');

QualifiedName returns string:
    ID ('.' ID)*;

terminal ID:
    /[_a-zA-Z][a-zA-Z0-9_]*/;

terminal LITERAL:
    /'[^']*'/ | /[0-9]+(\.[0-9]+)?/ | 'true' | 'false' | 'DELETED' | 'ANONYMOUS';

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
    name=ID ':' message=STRING ';';

WebAPIEndpoints:
    'WebAPIEndpoints' '{'
        (endpoints+=CustomEndpoint)*
    '}';

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

AggregateMetadata:
    '@Metadata' '{'
        (requiresAggregateState?='requiresAggregateState' ';')?
        (hasEventService?='hasEventService' ';')?
        (hasSagas?='hasSagas' ';')?
        (hasTransactions?='hasTransactions' ';')?
        (hasJpa?='hasJpa' ';')?
    '}';

ServiceDefinition:
    'Service' name=ID '{'
        (generateCrud?='@GenerateCrud' ';')?
        (transactional?='@Transactional' ';')?
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
    )?
    ';';

FunctionalityStep:
    SyncStep;

SyncStep:
    'sync' stepName=ID '{'
        stepActions+=StepAction*
    '}';

StepAction:
    FuncCallServiceAction | FuncSetResultAction;

FuncCallServiceAction:
    'call' serviceRef=ID '.' method=ID '(' (args+=ID (',' args+=ID)*)? ')' ';';

FuncSetResultAction:
    'set' 'result' 'to' resultType=ID ('as' getterName=ID)? ';';

ServiceMethod:
    (annotations+=Annotation)*
    name=ID '(' (parameters+=Parameter (',' parameters+=Parameter)*)? ')' 
    (':' returnType=ReturnType)? ('{' 
        implementation=MethodImplementation
    '}' | ';');

MethodImplementation:
    (actions+=BusinessAction)*;

BusinessAction:
    LoadAggregateAction | ValidateAction | CreateEntityAction | DomainOperationAction | 
    RegisterChangeAction | RegisterEventAction | PublishEventAction | CallServiceAction | 
    ThrowExceptionAction | ReturnAction;

LoadAggregateAction:
    'load' aggregateVar=ID '=' 'aggregate' '(' aggregateId=ID ')' ';';

ValidateAction:
    'validate' condition=STRING ('throw' exception=ID '(' (exceptionParams+=ID (',' exceptionParams+=ID)*)? ')')? ';';

CreateEntityAction:
    'create' entityVar=ID '=' entityType=ID '(' (constructorParams+=ID (',' constructorParams+=ID)*)? ')' ';';

DomainOperationAction:
    'execute' targetVar=ID '.' operationChain=OperationChain ';';

OperationChain:
    operations+=ChainOperation ('.' operations+=ChainOperation)*;

ChainOperation:
    methodName=ID ('(' (params+=ID (',' params+=ID)*)? ')')?;

RegisterChangeAction:
    'register' aggregateVar=ID 'in' unitOfWorkVar=ID ';';

RegisterEventAction:
    'registerEvent' eventType=[PublishedEvent:ID] '(' (eventParams+=ID (',' eventParams+=ID)*)? ')' 'in' unitOfWorkVar=ID ';';

PublishEventAction:
    'publish' eventType=[PublishedEvent:ID] '(' (eventParams+=ID (',' eventParams+=ID)*)? ')' ';';

CallServiceAction:
    'call' serviceVar=ID '=' service=ID '.' methodName=ID '(' (callParams+=ID (',' callParams+=ID)*)? ')' ';';

ThrowExceptionAction:
    'throw' exception=ID '(' (exceptionParams+=ID (',' exceptionParams+=ID)*)? ')' ';';

ReturnAction:
    'return' (returnValue=ID | returnExpression=STRING)? ';';

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

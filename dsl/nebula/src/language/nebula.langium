grammar Nebula

entry Model:
    imports+=Import*
    (aggregates+=Aggregate | sharedDtos+=SharedDtos)*
    (exceptions=ExceptionMessages)?;

Import:
    'import' sharedDtos?='shared-dtos' ';';

Aggregate:
    (annotations+=Annotation)*
    (metadata=AggregateMetadata)?
    'Aggregate' name=ID '{'
        entities+=Entity*
        methods+=Method*
        workflows+=Workflow*
        (customRepository=CustomRepository)?
        (webApiEndpoints=WebAPIEndpoints)?
        (serviceDefinition=ServiceDefinition)?
    '}';

Entity:
    (isRoot?='Root')? 'Entity' name=ID '{'
        properties+=Property*
        constructors+=Constructor*
        ('invariants' '{'
            invariants+=Invariant*
        '}')?
        ('businessRules' '{'
            rules+=BusinessRule*
        '}')?
        ('methods' '{'
            methods+=Method*
        '}')?
    '}';

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

RepositoryMethod:
    returnType=RepositoryReturnType name=ID '(' (parameters+=Parameter (',' parameters+=Parameter)*)? ')' ';';

RepositoryReturnType:
    OptionalType | ListType | SetType | ParamType;

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
    type=Type name=ID (',' names+=ID)* (isKey?='key')? ';';

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
    collection=PropertyReference '.' operation=('size' | 'isEmpty' | 'count' | 'distinct' | 'findAny' | 'findFirst');

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

BusinessRule:
    'rule' name=STRING '{'
        'trigger' ':' trigger=ID ';'
        'affectedFields' ':' '[' fields+=ID (',' fields+=ID)* ']' ';'
        'conditions' ':' '[' conditions+=STRING (',' conditions+=STRING)* ']' ';'
        'exception' ':' exception=STRING ';'
        ('bidirectional' ':' bidirectional=STRING ';')?
    '}';

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

ServiceMethod:
    (annotations+=Annotation)*
    name=ID '(' (parameters+=Parameter (',' parameters+=Parameter)*)? ')' 
    (':' returnType=ReturnType)? ';';

// Shared DTOs Support
SharedDtos:
    'SharedDtos' '{'
        dtos+=DtoDefinition*
    '}';

DtoDefinition:
    'dto' name=ID (genericParams=GenericParameters)? '{'
        fields+=DtoField*
    '}';

GenericParameters:
    '<' params+=ID (',' params+=ID)* '>';

DtoField:
    type=DtoFieldType name=ID ';';

DtoFieldType:
    PrimitiveType | DtoReference | ListType | SetType | OptionalType | GenericType;

DtoReference:
    dto=[DtoDefinition:ID];

GenericType:
    name=ID;




grammar Nebula

entry Model:
    imports+=Import*
    aggregates+=Aggregate*
    (exceptions=ExceptionMessages)?;

Import:
    'import' name=ID ';';

Aggregate:
    (annotations+=Annotation)*
    (metadata=AggregateMetadata)?
    'Aggregate' name=ID '{'
        entities+=Entity*
        methods+=Method*
        workflows+=Workflow*
        (customRepository=CustomRepository)?
        (validationRules=ValidationRules)?
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
    'Optional' '<' type=ParamType '>' | 
    'List' '<' type=ParamType '>' |
    'Set' '<' type=ParamType '>' |
    type=ParamType;

Parameter:
    (annotations+=Annotation)*
    type=ParamType name=ID;

Annotation:
    '@' name=ID ('(' (values+=AnnotationValue (',' values+=AnnotationValue)*)? ')')?;

AnnotationValue:
    key=ID '=' value=(STRING | ID | LITERAL) | 
    value=(STRING | ID | LITERAL);

ParamType:
    PrimitiveType | EntityType | BuiltinType | ListType;

BuiltinType:
    name=('UnitOfWork' | 'UserDto' | 'AggregateState');

ReturnType:
    'void' | PrimitiveType | EntityType | BuiltinType | ListType;

ListType:
    'List' '<' elementType=ID '>';

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
    PrimitiveType | CollectionType | EntityType | AggregateStateType;

PrimitiveType:
    typeName=('Integer' | 'Long' | 'String' | 'Boolean' | 'LocalDateTime');

CollectionType:
    'Set' '<' elementType=(EntityType | PrimitiveType) '>';

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

ValidationRules:
    'ValidationRules' '{'
        (typeValidations+=TypeValidation)*
        (businessRules+=ValidationBusinessRule)*
    '}';

TypeValidation:
    type=ParamType ':' (annotations+=STRING)*;

ValidationBusinessRule:
    'BusinessRule' name=ID '{'
        desc=STRING
        logic=STRING
    '}';

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
        (requiresUserDto?='requiresUserDto' ';')?
        (requiresAggregateState?='requiresAggregateState' ';')?
        (hasEventService?='hasEventService' ';')?
        (hasScheduling?='hasScheduling' ';')?
        (hasRetry?='hasRetry' ';')?
        (hasSagas?='hasSagas' ';')?
        (hasTransactions?='hasTransactions' ';')?
        (hasJpa?='hasJpa' ';')?
        (hasExternalDtos?='hasExternalDtos' ';')?
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
    name=ID '(' (parameters+=ServiceParameter (',' parameters+=ServiceParameter)*)? ')' 
    (':' returnType=ServiceReturnType)? ';';

ServiceParameter:
    type=ServiceParameterType name=ID;

ServiceParameterType:
    PrimitiveType | EntityType | 'UserDto' | 'AggregateState' | ListType;

ServiceReturnType:
    'void' | PrimitiveType | EntityType | 'UserDto' | 'AggregateState' | ListType;




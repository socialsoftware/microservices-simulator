grammar Common

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

QualifiedName returns string:
    ID ('.' ID)*;

PrimitiveType:
    typeName=('Integer' | 'Long' | 'Float' | 'Double' | 'String' | 'Boolean' | 'LocalDateTime');

AggregateStateType:
    typeName=('AggregateState');

OptionalType:
    'Optional' '<' elementType=(PrimitiveType | ID) '>';

BaseCollectionType:
    'Set' '<' elementType=PrimitiveType '>' |
    'List' '<' elementType=PrimitiveType '>';

BaseType:
    PrimitiveType | BaseCollectionType | AggregateStateType | OptionalType;

BaseParamType:
    BaseType | BuiltinType | ListType | SetType;

BuiltinType:
    name=('UnitOfWork' | 'AggregateState');

BaseReturnType:
    'void' | BaseType | BuiltinType | ListType | SetType;

ListType:
    'List' '<' elementType=(PrimitiveType | ID) '>';

SetType:
    'Set' '<' elementType=(PrimitiveType | ID) '>';

Annotation:
    '@' name=ID ('(' (values+=AnnotationValue (',' values+=AnnotationValue)*)? ')')?;

AnnotationValue:
    key=ID '=' value=(STRING | ID | LITERAL) | 
    value=(STRING | ID | LITERAL);


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
    value=(LITERAL | 'true' | 'false' | 'null');

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


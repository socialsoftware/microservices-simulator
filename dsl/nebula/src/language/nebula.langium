grammar Nebula

entry Model:
    aggregates+=Aggregate*;

Aggregate:
    'Aggregate' name=ID '{'
        entities+=Entity*
    '}';

Entity:
    (isRoot?='Root')? 'Entity' name=ID '{'
        properties+=Property*
        ('invariants' '{'
            invariants+=Invariant*
        '}')?
    '}';

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
        {infer MethodCall.receiver=current} '.' method=ID ('(' (arguments+=Expression (',' arguments+=Expression)*)? ')')? |
        {infer PropertyAccess.receiver=current} '.' member=ID |
        {infer LambdaCall.collection=current} '.' lambdaOp=ID '(' variable=ID '=>' body=Expression ')'
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
    type=[Entity:ID];

AggregateStateType:
    typeName=('AggregateState');

terminal ID:
    /[_a-zA-Z][a-zA-Z0-9_]*/;

terminal LITERAL:
    /'[^']*'/ | /[0-9]+(\.[0-9]+)?/ | 'true' | 'false' | 'DELETED' | 'ANONYMOUS';

hidden terminal WS:
    /\s+/;

hidden terminal ML_COMMENT:
    /\/\*([^*]|\*+[^*/])*\*+\//;

hidden terminal SL_COMMENT:
    /\/\/[^\n\r]*/;


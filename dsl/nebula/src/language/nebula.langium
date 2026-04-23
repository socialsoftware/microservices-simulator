grammar Nebula

entry Model:
    (aggregates+=Aggregate | sharedEnums+=SharedEnums | sagaStatesBlocks+=SagaStatesBlock | topLevelWorkflows+=TopLevelWorkflow)*
    (exceptions=ExceptionMessages)?;

SagaStatesBlock:
    'SagaStates' '{'
        groups+=SagaStateGroup*
    '}';

SagaStateGroup:
    name=ID '{'
        states+=ID (',' states+=ID)*
    '}';

TopLevelWorkflow:
    'Workflow' name=ID '{'
        ('input' '{'
            (inputs+=WorkflowInput (',' inputs+=WorkflowInput)*)?
        '}')?
        workflowSteps+=TopLevelWorkflowStep+
    '}';

WorkflowInput:
    type=ParamType name=ID;

TopLevelWorkflowStep:
    'step' name=ID '{'
        'action' ':' action=WorkflowActionCall
        ('compensate' ':' compensate=WorkflowActionCall)?
        ('lock' lockAggregate=ID '(' lockId=WorkflowCallArg ')' 'as' lockState=ID
            ('forbidden' '[' forbidden+=ID (',' forbidden+=ID)* ']')?)?
    '}';

WorkflowActionCall:
    target=ID '.' method=ID '(' (args+=WorkflowCallArg (',' args+=WorkflowCallArg)*)? ')';

WorkflowCallArg:
    name=ID ('.' chain+=ID)*;

Aggregate:
    (annotations+=Annotation)*
    'Aggregate' name=ID '{'
        (aggregateElements+=AggregateElement)*
    '}';

AggregateElement:
    Entity | Methods | Repository | Events;

Methods:
    'Methods' '{'
        (methods+=Method)*
    '}';

Entity:
    (isRoot?='Root')? (generateDto?='Dto')? 'Entity' name=ID
    ('from' aggregateRef=ID)?
    (
        '{'
            (properties+=Property | sourceBlocks+=SourceBlock)*
            ('invariants' '{'
                invariants+=Invariant*
            '}')?
        '}'
    )?;

SourceBlock:
    'from' aggregateRef=ID '{'
        (sources+=SourcedField (',' sources+=SourcedField)*)?
    '}';

SourcedField:
    dtoField=DtoFieldPath ('as' entityField=ID)?;

DtoFieldPath:
    parts+=ID ('.' parts+=ID)*;

Method:
    (annotations+=Annotation)*
    name=ID '(' (parameters+=Parameter (',' parameters+=Parameter)*)? ')'
    (':' returnType=ReturnType)?
    (
        '{' (rawBody=STRING | actionBody=ActionMethodBody | queryBody=QueryMethodBody) '}'
    )?;

QueryMethodBody:
    'query' repositoryMethod=ID '(' (args+=ActionExpression (',' args+=ActionExpression)*)? ')';

ActionMethodBody:
    ('precondition' '{' preconditions+=ActionPrecondition* '}')?
    'action' '{' statements+=ActionStatement* '}'
    (publishes+=PublishesClause)*;

PublishesClause:
    'publishes' eventType=ID '{'
        (assignments+=PublishesFieldAssignment (',' assignments+=PublishesFieldAssignment)*)?
    '}';

PublishesFieldAssignment:
    field=ID ':' value=ActionExpression;

ActionPrecondition:
    'check' left=ActionExpression op=ActionBoolOp right=ActionExpression
    'error' message=STRING;

ActionBoolOp returns string:
    '==' | '!=' | '<=' | '>=' | '<' | '>';

ActionStatement:
    CreateActionStatement | LoadActionStatement | FindActionStatement | AssignActionStatement;

FindActionStatement:
    'find' collection=ActionRef 'where' field=ID '==' value=ActionExpression 'as' alias=ID;

LoadActionStatement:
    'load' aggregateRef=ID '(' id=ActionExpression ')' 'as' alias=ID;

AssignActionStatement:
    aliasRef=ID '.' field=ID '=' value=ActionExpression;

CreateActionStatement:
    'create' aggregateRef=ID '{'
        (fields+=ActionFieldAssignment (',' fields+=ActionFieldAssignment)*)?
    '}';

ActionFieldAssignment:
    field=ID ':' value=ActionExpression;

ActionExpression:
    ActionLiteral | ActionRef;

ActionRef:
    name=ID ('.' chain+=ID)*;

ActionLiteral:
    stringValue=STRING | literalValue=LITERAL | boolLiteral=BooleanLiteral;

BooleanLiteral returns string:
    'true' | 'false';

Repository:
    'Repository' '{'
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
    'subscribe' eventType=ID
    ('from' sourceAggregate=ID)?
    ('routing' '(' routingIdExpr=Expression (',' routingVersionExpr=Expression)? ')')?
    (
        '{'
            ('when' whenCondition=Expression)?
            (reactionBody=ActionMethodBody)?
        '}'
    )?;

EventField:
    type=Type name=ID;

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
    (annotations+=Annotation)*
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
    conditions+=InvariantCondition ':' errorMessage=STRING;

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

PrimitiveType:
    typeName=('Integer' | 'Long' | 'Float' | 'Double' | 'String' | 'Boolean' | 'LocalDateTime' | 'Object');

BuiltinType:
    typeName=('UnitOfWork' | 'AggregateState');

EntityType:
    type=[Entity:QualifiedName];

BaseType:
    PrimitiveType | BuiltinType | EntityType;

ListType:
    'List' '<' elementType=BaseType '>';

SetType:
    'Set' '<' elementType=BaseType '>';

OptionalType:
    'Optional' '<' elementType=BaseType '>';

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

EnumDefinition:
    name=ID '{'
        values+=EnumValue (',' values+=EnumValue)*
    '}';

EnumValue:
    name=ID;

SharedEnums:
    'SharedEnums' '{'
        enums+=EnumDefinition*
    '}';

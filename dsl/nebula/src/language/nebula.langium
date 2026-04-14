grammar Nebula

entry Model:
    (aggregates+=Aggregate | sharedEnums+=SharedEnums | topLevelWorkflows+=TopLevelWorkflow)*
    (exceptions=ExceptionMessages)?;

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
    Entity | Method | Workflow | Repository | Events | WebAPIEndpoints | ServiceDefinition | Functionalities | StateMachine | SagaStates;

StateMachine:
    'States' name=ID '{'
        ('initial' initial=ID)?
        ('terminal' terminal+=ID (',' terminal+=ID)*)?
        transitions+=StateTransition*
    '}';

StateTransition:
    from=ID '->' to=ID
    ('publishes' eventType=ID
        ('{'
            (assignments+=PublishesFieldAssignment (',' assignments+=PublishesFieldAssignment)*)?
        '}')?
    )?;

SagaStates:
    'SagaStates' '{'
        states+=ID (',' states+=ID)*
    '}';

Entity:
    (isRoot?='Root')? (generateDto?='Dto')? 'Entity' name=ID
    ('from' aggregateRef=ID)?
    (
        '{'
            (properties+=Property | fieldMappings+=DtoFieldMapping | sourceBlocks+=SourceBlock)*
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

DtoFieldMapping:
    'map' dtoField=DtoFieldPath 'as' entityField=ID;

DtoFieldPath:
    parts+=ID ('.' parts+=ID)*;

Method:
    (annotations+=Annotation)*
    'method' name=ID '(' (parameters+=Parameter (',' parameters+=Parameter)*)? ')'
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
    CreateActionStatement | LoadActionStatement | FindActionStatement | AssignActionStatement | ExtensionActionStatement;

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

ExtensionActionStatement:
    'extension' fnName=ID '(' (args+=ActionExpression (',' args+=ActionExpression)*)? ')';

ActionExpression:
    ActionLiteral | ActionRef;

ActionRef:
    name=ID ('.' chain+=ID)*;

ActionLiteral:
    stringValue=STRING | literalValue=LITERAL | boolLiteral=BooleanLiteral;

BooleanLiteral returns string:
    'true' | 'false';

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

WorkflowStepAction:
    WorkflowCallAction
    | WorkflowExtractAction
    | WorkflowRegisterStateAction
    | WorkflowSetFieldAction;

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

WorkflowArg:
    ref=ID ('.' chain+=ID)*;

WorkflowType returns string:
    'saga';

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

FunctionalityStep:
    SyncStep;

SyncStep:
    'sync' stepName=ID
    (dependsOn=StepDependencies)?
    '{'
        stepActions+=StepAction*
    '}'
    (compensation=StepCompensation)?;

StepDependencies:
    'depends' 'on' dependencies+=ID (',' dependencies+=ID)*;

StepCompensation:
    'compensation' '{'
        compensationActions+=StepAction*
    '}';

StepAction:
    FuncCallServiceAction
    | FuncSetResultAction
    | FuncAssignVariableAction
    | FuncRegisterSagaStateAction;

FuncCallServiceAction:
    'call' serviceRef=ID '.' method=ID '(' (args+=StepArgument (',' args+=StepArgument)*)? ')'
    ('assign' 'to' assignTo=ID)?;

FuncSetResultAction:
    'set' 'result' 'to' resultType=ID ('as' getterName=ID)?;

FuncAssignVariableAction:
    'set' variableName=ID '=' expression=StepExpression;

FuncRegisterSagaStateAction:
    'register' 'sagaState' aggregateId=StepArgument 'as' sagaState=ID 'in' unitOfWork=ID;

StepArgument:
    {infer StepArgument} (STRING | ID);

StepExpression:
    STRING | LITERAL | Expression | ID;

ServiceMethod:
    (annotations+=Annotation)*
    name=ID '(' (parameters+=Parameter (',' parameters+=Parameter)*)? ')'
    (':' returnType=ReturnType)?;

EnumDefinition:
    'enum' name=ID '{'
        values+=EnumValue (',' values+=EnumValue)*
    '}';

EnumValue:
    name=ID;

SharedEnums:
    'SharedEnums' '{'
        enums+=EnumDefinition*
    '}';

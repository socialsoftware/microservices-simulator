grammar Service

import "./common"
import "./entity"
import "./events"

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


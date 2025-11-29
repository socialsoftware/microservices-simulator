grammar Functionality

import "./common"
import "./entity"

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
    FuncCallServiceAction | FuncSetResultAction | FuncAssignVariableAction | FuncRegisterSagaStateAction;

FuncCallServiceAction:
    'call' serviceRef=ID '.' method=ID '(' (args+=StepArgument (',' args+=StepArgument)*)? ')' 
    ('assign' 'to' assignTo=ID)? ';';

FuncSetResultAction:
    'set' 'result' 'to' resultType=ID ('as' getterName=ID)? ';';

FuncAssignVariableAction:
    'set' variableName=ID '=' expression=StepExpression ';';

FuncRegisterSagaStateAction:
    'register' 'sagaState' aggregateId=StepArgument 'as' sagaState=ID 'in' unitOfWork=ID ';';

StepArgument:
    {infer StepArgument} (STRING | ID);

StepExpression:
    STRING | LITERAL | Expression | ID;


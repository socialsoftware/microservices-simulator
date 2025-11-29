grammar Entity

import "./common"

EntityType:
    type=[Entity:QualifiedName];

Type:
    BaseType | EntityType;

CollectionType:
    BaseCollectionType |
    'Set' '<' elementType=EntityType '>' |
    'List' '<' elementType=EntityType '>';

ParamType:
    BaseParamType | EntityType;

ReturnType:
    BaseReturnType | EntityType;

Parameter:
    (annotations+=Annotation)*
    type=ParamType name=ID;

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

Property:
    (isFinal?='final')? type=Type name=ID (',' names+=ID)* ('default' defaultValue=PropertyDefaultValue)? (dtoExclude?='dto-exclude')? ';';

PropertyDefaultValue returns string:
    LITERAL
  | STRING
  | QualifiedName
  | AggregateStateQualified
  | 'null'
  | '[]';

AggregateStateQualified returns string:
    'AggregateState' '.' ID;

Invariant:
    'check' name=ID '{'
        conditions+=InvariantCondition*
    '}';

InvariantCondition:
    expression=Expression ';';

Method:
    (annotations+=Annotation)*
    'method' name=ID '(' (parameters+=Parameter (',' parameters+=Parameter)*)? ')' 
    (':' returnType=ReturnType)? 
    ('{' body=STRING '}' | ';');

Constructor:
    'constructor' name=ID '(' (parameters+=Parameter (',' parameters+=Parameter)*)? ')' 
    ('{' body=STRING '}' | ';');

DtoEntityMapping:
    'mapping' '{'
        fieldMappings+=DtoFieldMapping*
    '}';

DtoFieldMapping:
    dtoField=ID '->' entityField=ID ('extract' extractField=ID)? ';';


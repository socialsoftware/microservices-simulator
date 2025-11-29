grammar Workflow

import "./common"
import "./entity"

Workflow:
    (annotations+=Annotation)*
    workflowType=WorkflowType 'workflow' name=ID '(' (parameters+=Parameter (',' parameters+=Parameter)*)? ')' 
    (':' returnType=ReturnType)? ';';

WorkflowType returns string:
    'causal' | 'saga';


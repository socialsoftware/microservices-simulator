grammar Enums

import "./common"

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


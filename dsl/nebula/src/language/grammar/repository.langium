grammar Repository

import "./common"
import "./entity"

Repository:
    'Repository' '{'
        repositoryMethods+=RepositoryMethod*
    '}';

RepositoryMethod:
    ('@Query' '(' query=STRING ')')? returnType=RepositoryReturnType name=ID '(' (parameters+=Parameter (',' parameters+=Parameter)*)? ')' ';';

RepositoryReturnType:
    ParamType;


grammar WebAPI

import "./common"
import "./entity"

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


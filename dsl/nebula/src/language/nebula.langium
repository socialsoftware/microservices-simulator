grammar Nebula

import "./grammar/common"
import "./grammar/entity"
import "./grammar/repository"
import "./grammar/events"
import "./grammar/service"
import "./grammar/functionality"
import "./grammar/webapi"
import "./grammar/workflow"
import "./grammar/enums"

entry Model:
    imports+=Import*
    (aggregates+=Aggregate | sharedEnums+=SharedEnums)*
    (exceptions=ExceptionMessages)?;

Import:
    'import' sharedEnums?='shared-enums' ';';

Aggregate:
    (annotations+=Annotation)*
    (metadata=AggregateMetadata)?
    'Aggregate' name=ID '{'
        (aggregateElements+=AggregateElement)*
    '}';

AggregateElement:
    Entity | Method | Workflow | Repository | Events | WebAPIEndpoints | ServiceDefinition | Functionalities;

AggregateMetadata:
    '@Metadata' '{'
        (requiresAggregateState?='requiresAggregateState' ';')?
        (hasEventService?='hasEventService' ';')?
        (hasSagas?='hasSagas' ';')?
        (hasTransactions?='hasTransactions' ';')?
        (hasJpa?='hasJpa' ';')?
    '}';

ExceptionMessages:
    'exceptions' '{'
        messages+=ExceptionMessage*
    '}';

ExceptionMessage:
    name=ID ':' message=STRING ';';

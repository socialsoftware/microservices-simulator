grammar Events

import "./common"
import "./entity"

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
    'subscribe' eventType=[PublishedEvent:QualifiedName] 'from' sourceAggregate=ID '{'
        conditions+=EventCondition*
    '}';

EventField:
    type=Type name=ID ';';

EventCondition:
    condition=Expression? ';';


import { DefaultScopeComputation, LangiumDocument, AstNodeDescription } from 'langium';
import { CancellationToken } from 'vscode-languageserver';
import { isModel } from './generated/ast.js';
import { getEntities, getEvents } from '../cli/utils/aggregate-helpers.js';

export class NebulaScopeComputation extends DefaultScopeComputation {
    override async computeExports(document: LangiumDocument, cancelToken?: CancellationToken): Promise<AstNodeDescription[]> {
        const descriptions: AstNodeDescription[] = [];

        if (document.parseResult.value && isModel(document.parseResult.value)) {
            const model = document.parseResult.value;

            for (const aggregate of model.aggregates) {
                const entities = getEntities(aggregate);
                for (const entity of entities) {
                    descriptions.push(this.descriptions.createDescription(entity, entity.name));
                }
            }

            for (const sharedEnums of model.sharedEnums) {
                for (const enumDef of sharedEnums.enums) {
                    descriptions.push(this.descriptions.createDescription(enumDef, enumDef.name, document));
                }
            }

            for (const aggregate of model.aggregates) {
                const events = getEvents(aggregate);
                if (events?.publishedEvents) {
                    for (const publishedEvent of events.publishedEvents) {
                        descriptions.push(this.descriptions.createDescription(publishedEvent, publishedEvent.name, document));
                    }
                }
            }
        }

        return descriptions;
    }
} 
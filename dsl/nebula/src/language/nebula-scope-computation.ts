import { DefaultScopeComputation, LangiumDocument, AstNodeDescription } from 'langium';
import { CancellationToken } from 'vscode-languageserver';
import { isModel } from './generated/ast.js';
import { getEntities, getEvents } from '../cli/utils/aggregate-helpers.js';

export class NebulaScopeComputation extends DefaultScopeComputation {
    override async computeExports(document: LangiumDocument, cancelToken?: CancellationToken): Promise<AstNodeDescription[]> {
        const descriptions: AstNodeDescription[] = [];

        if (document.parseResult.value && isModel(document.parseResult.value)) {
            const model = document.parseResult.value;

            // Export all entities from all aggregates
            for (const aggregate of model.aggregates) {
                const entities = getEntities(aggregate);
                for (const entity of entities) {
                    descriptions.push(this.descriptions.createDescription(entity, entity.name));
                }
            }

            // Export all DTOs from SharedDtos blocks as if they were entities
            // This allows them to be referenced through EntityType
            for (const sharedDtos of model.sharedDtos) {
                for (const dto of sharedDtos.dtos) {
                    // Create a description that makes the DTO resolvable as an Entity
                    descriptions.push(this.descriptions.createDescription(dto, dto.name, document));
                }
            }

            // Export all enum definitions from SharedEnums blocks
            // This allows them to be referenced through EntityType
            for (const sharedEnums of model.sharedEnums) {
                for (const enumDef of sharedEnums.enums) {
                    // Create a description that makes the enum resolvable through EntityType
                    descriptions.push(this.descriptions.createDescription(enumDef, enumDef.name, document));
                }
            }

            // Export all published events from all aggregates
            // This allows them to be referenced in subscribed events across aggregates
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
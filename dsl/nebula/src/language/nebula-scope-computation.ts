import { DefaultScopeComputation, LangiumDocument, AstNodeDescription } from 'langium';
import { CancellationToken } from 'vscode-languageserver';
import { isModel } from './generated/ast.js';

export class NebulaScopeComputation extends DefaultScopeComputation {
    override async computeExports(document: LangiumDocument, cancelToken?: CancellationToken): Promise<AstNodeDescription[]> {
        const descriptions: AstNodeDescription[] = [];

        if (document.parseResult.value && isModel(document.parseResult.value)) {
            const model = document.parseResult.value;

            // Export all entities from all aggregates
            for (const aggregate of model.aggregates) {
                for (const entity of aggregate.entities) {
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
        }

        return descriptions;
    }
} 
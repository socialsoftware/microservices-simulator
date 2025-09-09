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
        }

        return descriptions;
    }
} 
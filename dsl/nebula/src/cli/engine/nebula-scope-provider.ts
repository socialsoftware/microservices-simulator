import {
    ReferenceInfo,
    Scope,
    DefaultScopeProvider,
    AstUtils,
    LangiumCoreServices,
    AstNodeDescriptionProvider,
    MapScope,
    AstNodeDescription
} from "langium";
import {
    isEntityType,
    Model
} from "../../language/generated/ast.js";
import { getEntities } from "../utils/aggregate-helpers.js";

export class NebulaScopeProvider extends DefaultScopeProvider {
    private astNodeDescriptionProvider: AstNodeDescriptionProvider;
    private services: LangiumCoreServices;

    constructor(services: LangiumCoreServices) {
        super(services);
        this.services = services;
        this.astNodeDescriptionProvider = services.workspace.AstNodeDescriptionProvider;
    }

    override getScope(context: ReferenceInfo): Scope {
        if (isEntityType(context.container) && context.property === 'type') {
            const document = AstUtils.getDocument(context.container);
            const model = document.parseResult.value as Model;

            const descriptions: AstNodeDescription[] = [];

            if (model.aggregates) {
                for (const aggregate of model.aggregates) {
                    const entities = getEntities(aggregate);
                    for (const entity of entities) {
                        if (entity.name) {
                            const desc = this.astNodeDescriptionProvider.createDescription(entity, entity.name);
                            descriptions.push(desc);
                        }
                    }
                }
            }

            if (model.sharedEnums) {
                for (const sharedEnum of model.sharedEnums) {
                    if (sharedEnum.enums) {
                        for (const enumDef of sharedEnum.enums) {
                            if (enumDef.name) {
                                const desc = this.astNodeDescriptionProvider.createDescription(enumDef, enumDef.name);
                                descriptions.push(desc);
                            }
                        }
                    }
                }
            }

            try {
                const indexManager = (this.services as any).shared?.workspace?.IndexManager;
                if (indexManager) {
                    const allDescriptions = indexManager.allElements();
                    const enumDescriptions = allDescriptions.filter((desc: AstNodeDescription) =>
                        desc.type === 'EnumDefinition'
                    );
                    descriptions.push(...enumDescriptions);
                } else {
                    const globalScope = this.getGlobalScope('EnumDefinition', context);
                    const globalEnumDescriptions = globalScope.getAllElements().filter(desc =>
                        desc.type === 'EnumDefinition'
                    );
                    descriptions.push(...globalEnumDescriptions);
                }
            } catch (error) {
            }

            if (descriptions.length > 0) {
                return new MapScope(descriptions);
            }
        }

        if (context.container.$type === 'SubscribedEvent' && context.property === 'eventType') {
            const descriptions: AstNodeDescription[] = [];

            const globalScope = super.getScope(context);
            const globalDescriptions = globalScope.getAllElements().filter(desc =>
                desc.type === 'PublishedEvent'
            );
            descriptions.push(...globalDescriptions);

            return new MapScope(descriptions);
        }

        return super.getScope(context);
    }
}

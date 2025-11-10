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

export class NebulaScopeProvider extends DefaultScopeProvider {
    private astNodeDescriptionProvider: AstNodeDescriptionProvider;
    private services: LangiumCoreServices;

    constructor(services: LangiumCoreServices) {
        super(services);
        this.services = services;
        this.astNodeDescriptionProvider = services.workspace.AstNodeDescriptionProvider;
    }

    override getScope(context: ReferenceInfo): Scope {
        // Check if we're dealing with a dtoType reference in Entity
        if (context.property === 'dtoType') {
            // This is a dtoType reference that should point to DtoDefinition
            const document = AstUtils.getDocument(context.container);
            const model = document.parseResult.value as Model;

            const descriptions: AstNodeDescription[] = [];

            // Collect all DTO definitions from SharedDtos in the current document
            if (model.sharedDtos) {
                for (const sharedDtosBlock of model.sharedDtos) {
                    if (sharedDtosBlock.dtos) {
                        for (const dto of sharedDtosBlock.dtos) {
                            if (dto.name) {
                                const desc = this.astNodeDescriptionProvider.createDescription(dto, dto.name);
                                descriptions.push(desc);
                            }
                        }
                    }
                }
            }

            // Also collect DTOs from imported shared-dtos files
            const globalScope = super.getScope(context);
            const globalDescriptions = globalScope.getAllElements().filter(desc =>
                desc.type === 'DtoDefinition'
            );
            descriptions.push(...globalDescriptions);

            return new MapScope(descriptions);
        }

        // Check if we're dealing with an EntityType reference
        if (isEntityType(context.container) && context.property === 'type') {
            // This is an EntityType reference that could point to either Entity or EnumDefinition

            // Get the document root
            const document = AstUtils.getDocument(context.container);
            const model = document.parseResult.value as Model;

            const descriptions: AstNodeDescription[] = [];

            // 1. Collect all entities from aggregates in the current document
            if (model.aggregates) {
                for (const aggregate of model.aggregates) {
                    if (aggregate.entities) {
                        for (const entity of aggregate.entities) {
                            if (entity.name) {
                                const desc = this.astNodeDescriptionProvider.createDescription(entity, entity.name);
                                descriptions.push(desc);
                            }
                        }
                    }
                }
            }

            // 2. Collect all SharedDto entities from the current document
            if (model.sharedDtos) {
                for (const sharedDto of model.sharedDtos) {
                    if (sharedDto.dtos) {
                        for (const dto of sharedDto.dtos) {
                            if (dto.name) {
                                const desc = this.astNodeDescriptionProvider.createDescription(dto, dto.name);
                                descriptions.push(desc);
                            }
                        }
                    }
                }
            }

            // 3. Collect all enum definitions from SharedEnums in the current document
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

            // 4. Always include all globally exported enum definitions from all files
            // This makes shared enums available without requiring explicit imports
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

        // Check if we're dealing with an eventType reference in SubscribedEvent
        if (context.container.$type === 'SubscribedEvent' && context.property === 'eventType') {
            const descriptions: AstNodeDescription[] = [];

            // Collect PublishedEvent from all aggregates in the workspace
            const globalScope = super.getScope(context);
            const globalDescriptions = globalScope.getAllElements().filter(desc =>
                desc.type === 'PublishedEvent'
            );
            descriptions.push(...globalDescriptions);

            return new MapScope(descriptions);
        }

        // Fall back to the default scope provider for other references
        return super.getScope(context);
    }
}

import { EntityExt, TypeGuards } from "../../../types/ast-extensions.js";
import { EntityGenerationOptions } from "./types.js";
import { generateFields } from "./fields.js";
import { generateDefaultConstructor, generateEntityDtoConstructor, generateCopyConstructor, generateProjectionDtoConstructor } from "./constructors.js";
import { generateGettersSetters, generateBackReferenceGetterSetter } from "./methods.js";
import { generateInvariants } from "./invariants.js";
import { ImportManager, ImportManagerFactory } from "../../../utils/import-manager.js";
import { ErrorHandler, ErrorUtils, ErrorSeverity } from "../../../utils/error-handler.js";
import type { DtoSchemaRegistry } from "../../../services/dto-schema-service.js";
import { getEffectiveProperties } from "../../../utils/aggregate-helpers.js";
import { ImportScanner } from "./builders/import-scanner.js";
import { InterInvariantBuilder } from "./builders/inter-invariant-builder.js";
import { EventSubscriptionBuilder } from "./builders/event-subscription-builder.js";
import { ClassAssembler } from "./builders/class-assembler.js";
import { DtoMethodBuilder } from "./builders/dto-method-builder.js";





export class EntityOrchestrator {
    private importManager: ImportManager;
    private importScanner?: ImportScanner;
    private dtoMethodBuilder?: DtoMethodBuilder;
    private interInvariantBuilder: InterInvariantBuilder;
    private eventSubscriptionBuilder: EventSubscriptionBuilder;
    private classAssembler: ClassAssembler;
    private dtoRegistry?: DtoSchemaRegistry;

    constructor(projectName: string) {
        this.importManager = ImportManagerFactory.createForMicroservice(projectName);
        this.interInvariantBuilder = new InterInvariantBuilder();
        this.eventSubscriptionBuilder = new EventSubscriptionBuilder();
        this.classAssembler = new ClassAssembler();
    }

    private getImportScanner(): ImportScanner {
        if (!this.importScanner) {
            this.importScanner = new ImportScanner(this.importManager, this.dtoRegistry!);
        }
        return this.importScanner;
    }

    private getDtoMethodBuilder(): DtoMethodBuilder {
        if (!this.dtoMethodBuilder) {
            this.dtoMethodBuilder = new DtoMethodBuilder(this.dtoRegistry, this.getImportScanner());
        }
        return this.dtoMethodBuilder;
    }

    generateEntityCode(entity: EntityExt, projectName: string, options?: EntityGenerationOptions): string {
        return ErrorHandler.wrap(
            () => this.generateEntityCodeInternal(entity, projectName, options),
            ErrorUtils.entityContext(
                'generate entity code',
                entity.$container?.name || 'unknown',
                entity.name,
                'entity-orchestrator',
                { isRoot: entity.isRoot, projectName }
            ),
            ErrorSeverity.FATAL
        ) || '';
    }

    private generateEntityCodeInternal(entity: EntityExt, projectName: string, options?: EntityGenerationOptions): string {
        const opts = options || { projectName };
        const isRootEntity = TypeGuards.isRootEntity(entity);

        this.dtoRegistry = opts.dtoSchemaRegistry;

        const components = this.generateEntityComponents(entity, projectName, opts, isRootEntity);

        const classStructure = this.classAssembler.buildClassStructure(entity, projectName, isRootEntity);

        const javaCode = this.classAssembler.assembleJavaCode(classStructure, components, entity.name);

        return this.getImportScanner().finalizeWithImports(javaCode, projectName, isRootEntity, classStructure.aggregateName, entity.name, entity);
    }

    private generateEntityComponents(entity: EntityExt, projectName: string, opts: EntityGenerationOptions, isRootEntity: boolean) {
        
        const effectiveProps = getEffectiveProperties(entity);

        
        const projectionDtoResult = generateProjectionDtoConstructor(entity, projectName, this.dtoRegistry);

        return {
            fields: generateFields(effectiveProps, entity, isRootEntity, projectName).code,
            defaultConstructor: generateDefaultConstructor(entity).code,
            dtoConstructor: generateEntityDtoConstructor(entity, projectName, this.dtoRegistry).code,
            projectionDtoConstructor: projectionDtoResult?.code || '',
            copyConstructor: generateCopyConstructor(entity).code,
            gettersSetters: generateGettersSetters(effectiveProps, entity, projectName, opts.allEntities).code,
            backRefGetterSetter: (!isRootEntity && entity.$container)
                ? generateBackReferenceGetterSetter(entity.$container.name)
                : '',
            invariants: isRootEntity ? generateInvariants(entity).code : '',
            
            eventSubscriptions: isRootEntity ? this.eventSubscriptionBuilder.generateEventSubscriptionsMethod(entity.$container as any) : '',
            
            interInvariantMethods: isRootEntity ? this.interInvariantBuilder.generateInterInvariantMethods(entity.$container as any) : '',
            
            buildDtoMethod: this.getDtoMethodBuilder().generateBuildDtoMethod(entity)
        };
    }
}





export class EntityGenerator {
    private orchestrator: EntityOrchestrator;

    constructor(projectName: string = 'project') {
        this.orchestrator = new EntityOrchestrator(projectName);
    }

    

    async generateEntity(entity: EntityExt, options: EntityGenerationOptions): Promise<string> {
        return this.orchestrator.generateEntityCode(entity, options.projectName, options);
    }
}
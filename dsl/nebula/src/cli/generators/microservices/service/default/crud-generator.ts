import { Aggregate } from "../../../../../language/generated/ast.js";
import { CrudCreateGenerator } from "./crud-create-generator.js";
import { CrudReadGenerator } from "./crud-read-generator.js";
import { CrudReadAllGenerator } from "./crud-read-all-generator.js";
import { CrudUpdateGenerator } from "./crud-update-generator.js";
import { CrudDeleteGenerator } from "./crud-delete-generator.js";

/**
 * Service CRUD Generator (Orchestrator)
 *
 * Orchestrates the generation of all 5 CRUD methods for service classes.
 * Uses the Template Method pattern via specialized generators.
 *
 * Generates:
 * - create{Aggregate}(CreateRequestDto, UnitOfWork): EntityDto
 * - get{Aggregate}ById(Integer, UnitOfWork): EntityDto
 * - getAll{Aggregate}s(UnitOfWork): List<EntityDto>
 * - update{Aggregate}(EntityDto, UnitOfWork): EntityDto
 * - delete{Aggregate}(Integer, UnitOfWork): void
 *
 * Architecture:
 * - This class is a facade/orchestrator
 * - Each CRUD operation has its own generator extending MethodGeneratorTemplate
 * - Promotes single responsibility and consistent structure
 */
export class ServiceCrudGenerator {

    private createGenerator: CrudCreateGenerator;
    private readGenerator: CrudReadGenerator;
    private readAllGenerator: CrudReadAllGenerator;
    private updateGenerator: CrudUpdateGenerator;
    private deleteGenerator: CrudDeleteGenerator;

    constructor() {
        this.createGenerator = new CrudCreateGenerator();
        this.readGenerator = new CrudReadGenerator();
        this.readAllGenerator = new CrudReadAllGenerator();
        this.updateGenerator = new CrudUpdateGenerator();
        this.deleteGenerator = new CrudDeleteGenerator();
    }

    /**
     * Generate all CRUD methods for an aggregate.
     *
     * @param aggregateName Aggregate name
     * @param rootEntity Root entity (legacy parameter, extracted from aggregate in new generators)
     * @param projectName Project name for exception handling
     * @param aggregate Full aggregate (contains all metadata)
     * @returns Concatenated CRUD methods as a single string
     */
    static generateCrudMethods(aggregateName: string, rootEntity: any, projectName: string, aggregate?: Aggregate): string {
        if (!aggregate) {
            throw new Error('Aggregate is required for CRUD generation');
        }

        const generator = new ServiceCrudGenerator();
        const options = { projectName };

        // Generate each CRUD method using specialized generators
        const createMethod = generator.createGenerator.generate(aggregate, options);
        const readMethod = generator.readGenerator.generate(aggregate, options);
        const readAllMethod = generator.readAllGenerator.generate(aggregate, options);
        const updateMethod = generator.updateGenerator.generate(aggregate, options);
        const deleteMethod = generator.deleteGenerator.generate(aggregate, options);

        // Concatenate all methods with blank lines between them
        return `${createMethod}

${readMethod}

${readAllMethod}

${updateMethod}

${deleteMethod}`;
    }
}

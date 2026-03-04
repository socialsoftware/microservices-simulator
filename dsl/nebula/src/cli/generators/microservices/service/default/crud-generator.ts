import { Aggregate } from "../../../../../language/generated/ast.js";
import { CrudCreateGenerator } from "./crud-create-generator.js";
import { CrudReadGenerator } from "./crud-read-generator.js";
import { CrudReadAllGenerator } from "./crud-read-all-generator.js";
import { CrudUpdateGenerator } from "./crud-update-generator.js";
import { CrudDeleteGenerator } from "./crud-delete-generator.js";



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

    

    static generateCrudMethods(aggregateName: string, rootEntity: any, projectName: string, aggregate?: Aggregate): string {
        if (!aggregate) {
            throw new Error('Aggregate is required for CRUD generation');
        }

        const generator = new ServiceCrudGenerator();
        const options = { projectName };

        
        const createMethod = generator.createGenerator.generate(aggregate, options);
        const readMethod = generator.readGenerator.generate(aggregate, options);
        const readAllMethod = generator.readAllGenerator.generate(aggregate, options);
        const updateMethod = generator.updateGenerator.generate(aggregate, options);
        const deleteMethod = generator.deleteGenerator.generate(aggregate, options);

        
        return `${createMethod}

${readMethod}

${readAllMethod}

${updateMethod}

${deleteMethod}`;
    }
}

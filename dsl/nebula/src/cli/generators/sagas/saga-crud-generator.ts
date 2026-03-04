import type { Aggregate } from '../../../language/generated/ast.js';
import { SagaGenerationOptions } from './saga-generator.js';
import { SagaCreateGenerator } from './operations/saga-create-generator.js';
import { SagaReadGenerator } from './operations/saga-read-generator.js';
import { SagaReadAllGenerator } from './operations/saga-read-all-generator.js';
import { SagaUpdateGenerator } from './operations/saga-update-generator.js';
import { SagaDeleteGenerator } from './operations/saga-delete-generator.js';



export class SagaCrudGenerator {
    private createGenerator: SagaCreateGenerator;
    private readGenerator: SagaReadGenerator;
    private readAllGenerator: SagaReadAllGenerator;
    private updateGenerator: SagaUpdateGenerator;
    private deleteGenerator: SagaDeleteGenerator;

    constructor() {
        this.createGenerator = new SagaCreateGenerator();
        this.readGenerator = new SagaReadGenerator();
        this.readAllGenerator = new SagaReadAllGenerator();
        this.updateGenerator = new SagaUpdateGenerator();
        this.deleteGenerator = new SagaDeleteGenerator();
    }

    

    generateCrudSagaFunctionalities(
        aggregate: any,
        options: SagaGenerationOptions,
        packageName: string,
        allAggregates?: Aggregate[]
    ): Record<string, string> {
        const outputs: Record<string, string> = {};

        
        const createResult = this.createGenerator.generate(aggregate, options, packageName, allAggregates);
        outputs[createResult.fileName] = createResult.content;

        
        const readResult = this.readGenerator.generate(aggregate, options, packageName, allAggregates);
        outputs[readResult.fileName] = readResult.content;

        
        const readAllResult = this.readAllGenerator.generate(aggregate, options, packageName, allAggregates);
        outputs[readAllResult.fileName] = readAllResult.content;

        
        const updateResult = this.updateGenerator.generate(aggregate, options, packageName, allAggregates);
        outputs[updateResult.fileName] = updateResult.content;

        
        const deleteResult = this.deleteGenerator.generate(aggregate, options, packageName, allAggregates);
        outputs[deleteResult.fileName] = deleteResult.content;

        return outputs;
    }
}

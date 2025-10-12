import { BaseGenerationOptions } from '../../../base/types.js';
import { TestGenerationOptions } from './test-types.js';
import { UnitTestGenerator } from './unit-test-generator.js';
import { SagaTestGenerator } from './saga-test-generator.js';
import { ApiTestGenerator } from './api-test-generator.js';
import { TestDataBuilder } from './test-data-builder.js';

export { TestGenerationOptions } from './test-types.js';

export class TestGenerator {
    private unitTestGenerator = new UnitTestGenerator();
    private sagaTestGenerator = new SagaTestGenerator();
    private apiTestGenerator = new ApiTestGenerator();
    private testDataBuilder = new TestDataBuilder();

    async generate(options: TestGenerationOptions, context: BaseGenerationOptions): Promise<void> {
        const { features = [] } = options;

        // Generate unit tests (always generated)
        await this.unitTestGenerator.generateUnitTests(options);

        // Generate saga coordination tests if saga feature is enabled
        if (features.includes('saga')) {
            await this.sagaTestGenerator.generateSagaTests(options);
        }

        // Generate REST API tests if webapi feature is enabled
        if (features.includes('webapi')) {
            await this.apiTestGenerator.generateApiTests(options);
        }

        // Generate test data builders (always generated)
        await this.testDataBuilder.generateTestDataBuilders(options);

        console.log(`        - Generated test templates`);
    }
}

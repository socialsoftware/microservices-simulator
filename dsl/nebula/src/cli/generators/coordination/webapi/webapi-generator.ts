import { Aggregate } from "../../../../language/generated/ast.js";
import { WebApiGenerationOptions } from "./webapi-types.js";
import { ControllerGenerator } from "./controller-generator.js";
import { WebApiDtoGenerator } from "./dto-generator.js";
import { GlobalControllerGenerator } from "./global-controller-generator.js";
import { UtilityServiceGenerator } from "./utility-service-generator.js";

export { WebApiGenerationOptions } from "./webapi-types.js";

export class WebApiGenerator {
    private controllerGenerator = new ControllerGenerator();
    private dtoGenerator = new WebApiDtoGenerator();
    private globalControllerGenerator = new GlobalControllerGenerator();
    private utilityServiceGenerator = new UtilityServiceGenerator();

    async generateWebApi(aggregate: Aggregate, options: WebApiGenerationOptions, allAggregates?: Aggregate[]): Promise<{ [key: string]: string | Record<string, string> }> {
        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        if (!rootEntity) {
            throw new Error(`No root entity found in aggregate ${aggregate.name}`);
        }

        const results: { [key: string]: string | Record<string, string> } = {};

        results['controller'] = await this.controllerGenerator.generateController(aggregate, rootEntity, options, allAggregates);
        results['request-dtos'] = await this.dtoGenerator.generateRequestDtos(aggregate, rootEntity, options, allAggregates);
        results['response-dtos'] = await this.dtoGenerator.generateResponseDtos(aggregate, rootEntity, options);

        return results;
    }

    async generateEmptyController(aggregate: Aggregate, options: WebApiGenerationOptions): Promise<string> {
        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        if (!rootEntity) {
            throw new Error(`No root entity found in aggregate ${aggregate.name}`);
        }

        return await this.controllerGenerator.generateEmptyController(aggregate, options);
    }

    async generateGlobalControllers(options: WebApiGenerationOptions): Promise<{ [key: string]: string }> {
        const results: { [key: string]: string } = {};

        results['behaviour-controller'] = await this.globalControllerGenerator.generateBehaviourController(options);
        results['traces-controller'] = await this.globalControllerGenerator.generateTracesController(options);

        return results;
    }

    async generateUtilityServices(options: WebApiGenerationOptions): Promise<{ [key: string]: string }> {
        const results: { [key: string]: string } = {};

        results['behaviour-service'] = await this.utilityServiceGenerator.generateBehaviourService(options);
        results['traces-service'] = await this.utilityServiceGenerator.generateTracesService(options);

        return results;
    }
}

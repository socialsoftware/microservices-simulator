import { AggregateExt, TypeGuards } from "../../../types/ast-extensions.js";
import { WebApiGenerationOptions } from "./webapi-types.js";
import { ControllerGenerator } from "./controller-generator.js";
import { WebApiDtoGenerator } from "./dto-generator.js";

export { WebApiGenerationOptions } from "./webapi-types.js";

export class WebApiGenerator {
    private controllerGenerator = new ControllerGenerator();
    private dtoGenerator = new WebApiDtoGenerator();

    async generateWebApi(aggregate: AggregateExt, options: WebApiGenerationOptions, allAggregates?: AggregateExt[]): Promise<{ [key: string]: string | Record<string, string> }> {
        const rootEntity = aggregate.entities.find((e: any) => TypeGuards.isRootEntity(e));
        if (!rootEntity) {
            throw new Error(`No root entity found in aggregate ${aggregate.name}`);
        }

        const results: { [key: string]: string | Record<string, string> } = {};

        results['controller'] = await this.controllerGenerator.generateController(aggregate, rootEntity, options, allAggregates);
        results['request-dtos'] = await this.dtoGenerator.generateRequestDtos(aggregate, rootEntity, options, allAggregates);
        results['response-dtos'] = await this.dtoGenerator.generateResponseDtos(aggregate, rootEntity, options);

        return results;
    }

    async generateEmptyController(aggregate: AggregateExt, options: WebApiGenerationOptions): Promise<string> {
        const rootEntity = aggregate.entities.find((e: any) => TypeGuards.isRootEntity(e));
        if (!rootEntity) {
            throw new Error(`No root entity found in aggregate ${aggregate.name}`);
        }

        return await this.controllerGenerator.generateEmptyController(aggregate, options);
    }

}

import { WebApiGenerationOptions } from "./webapi-types.js";
import { WebApiBaseGenerator } from "./webapi-base-generator.js";

export class GlobalControllerGenerator extends WebApiBaseGenerator {
    async generateBehaviourController(options: WebApiGenerationOptions): Promise<string> {
        const context = this.buildBehaviourControllerContext(options);
        const template = this.getBehaviourControllerTemplate();
        return this.renderTemplate(template, context);
    }

    async generateTracesController(options: WebApiGenerationOptions): Promise<string> {
        const context = this.buildTracesControllerContext(options);
        const template = this.getTracesControllerTemplate();
        return this.renderTemplate(template, context);
    }

    private buildBehaviourControllerContext(options: WebApiGenerationOptions): any {
        return {
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.coordination.webapi`,
            basePackage: this.getBasePackage(),
            projectName: options.projectName.toLowerCase(),
            ProjectName: this.capitalize(options.projectName)
        };
    }

    private buildTracesControllerContext(options: WebApiGenerationOptions): any {
        return {
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.coordination.webapi`,
            basePackage: this.getBasePackage(),
            projectName: options.projectName.toLowerCase(),
            ProjectName: this.capitalize(options.projectName)
        };
    }

    private getBehaviourControllerTemplate(): string {
        return this.loadTemplate('web/behavior-controller.hbs');
    }

    private getTracesControllerTemplate(): string {
        return this.loadTemplate('web/traces-controller.hbs');
    }
}

import { WebApiGenerationOptions } from "./webapi-types.js";
import { WebApiBaseGenerator } from "./webapi-base-generator.js";

export class UtilityServiceGenerator extends WebApiBaseGenerator {
    async generateBehaviourService(options: WebApiGenerationOptions): Promise<string> {
        const context = this.buildUtilityServiceContext(options);
        const template = this.getBehaviourServiceTemplate();
        return this.renderTemplate(template, context);
    }

    async generateTracesService(options: WebApiGenerationOptions): Promise<string> {
        const context = this.buildUtilityServiceContext(options);
        const template = this.getTracesServiceTemplate();
        return this.renderTemplate(template, context);
    }

    private buildUtilityServiceContext(options: WebApiGenerationOptions): any {
        return {
            packageName: 'pt.ulisboa.tecnico.socialsoftware.ms',
            projectName: options.projectName.toLowerCase(),
            ProjectName: this.capitalize(options.projectName)
        };
    }

    private getBehaviourServiceTemplate(): string {
        return this.loadTemplate('web/behavior-service.hbs');
    }

    private getTracesServiceTemplate(): string {
        return this.loadTemplate('web/traces-service.hbs');
    }
}

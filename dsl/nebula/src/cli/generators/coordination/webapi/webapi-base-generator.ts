import { UnifiedTypeResolver } from "../../common/unified-type-resolver.js";
import { TemplateManager } from "../../../utils/template-manager.js";
import { WebApiGenerationOptions } from "../../microservices/types.js";
import Handlebars from "handlebars";

export abstract class WebApiBaseGenerator {
    // Helper methods
    protected capitalize(str: string): string {
        if (!str) return '';
        return str.charAt(0).toUpperCase() + str.slice(1);
    }

    protected getBasePackage(options: WebApiGenerationOptions): string {
        if (!options.basePackage) {
            throw new Error('basePackage is required in WebApiGenerationOptions');
        }
        return options.basePackage;
    }

    protected loadTemplate(templatePath: string): string {
        const templateManager = TemplateManager.getInstance();
        return templateManager.loadRawTemplate(templatePath);
    }

    protected renderTemplate(template: string, context: any): string {
        const compiledTemplate = Handlebars.compile(template, { noEscape: true });
        return compiledTemplate(context);
    }
    protected resolveHttpMethod(method: string | any): string {
        if (!method || typeof method !== 'string') return 'Get';
        const upperMethod = method.toUpperCase();
        switch (upperMethod) {
            case 'GET': return 'Get';
            case 'POST': return 'Post';
            case 'PUT': return 'Put';
            case 'DELETE': return 'Delete';
            case 'PATCH': return 'Patch';
            default: return 'Get';
        }
    }

    protected resolveParameterType(type: any): string {
        return UnifiedTypeResolver.resolveForWebApi(type);
    }

}

import { UnifiedTypeResolver } from "../../common/unified-type-resolver.js";
import { GeneratorBase } from "../../common/base/generator-base.js";
import { WebApiGenerationOptions } from "../../microservices/types.js";
import Handlebars from "handlebars";

export abstract class WebApiBaseGenerator extends GeneratorBase {
    

    protected getWebApiBasePackage(options: WebApiGenerationOptions): string {
        if (!options.basePackage) {
            throw new Error('basePackage is required in WebApiGenerationOptions');
        }
        return options.basePackage;
    }

    protected loadRawTemplate(templatePath: string): string {
        return this.templateManager.loadRawTemplate(templatePath);
    }

    protected renderTemplateFromString(template: string, context: any): string {
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

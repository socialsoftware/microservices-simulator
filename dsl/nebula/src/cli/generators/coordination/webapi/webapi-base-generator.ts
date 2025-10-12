import { OrchestrationBase } from "../../common/orchestration-base.js";
import { UnifiedTypeResolver } from "../../common/unified-type-resolver.js";

export abstract class WebApiBaseGenerator extends OrchestrationBase {
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

import { OrchestrationBase } from "../../base/orchestration-base.js";

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
        if (!type) return 'String';
        return this.resolveJavaType(type);
    }
}

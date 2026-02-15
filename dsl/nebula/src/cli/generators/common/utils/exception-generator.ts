

export class ExceptionGenerator {

    

    static generateCatchBlock(projectName: string, action: string, entityName: string): string {
        const capitalizedProject = this.capitalize(projectName);
        return `        } catch (${capitalizedProject}Exception e) {
            throw e;
        } catch (Exception e) {
            throw new ${capitalizedProject}Exception("Error ${action} ${entityName}: " + e.getMessage());
        }`;
    }

    

    static generateTryCatchWrapper(projectName: string, action: string, entityName: string, body: string): string {
        return `        try {
${body}
${this.generateCatchBlock(projectName, action, entityName)}`;
    }

    

    static generateCatchBlockWithMessage(projectName: string, customMessage: string): string {
        const capitalizedProject = this.capitalize(projectName);
        return `        } catch (${capitalizedProject}Exception e) {
            throw e;
        } catch (Exception e) {
            throw new ${capitalizedProject}Exception("${customMessage}: " + e.getMessage());
        }`;
    }

    

    static generateSimpleCatchBlock(projectName: string, action: string, entityName: string): string {
        const capitalizedProject = this.capitalize(projectName);
        return `        } catch (Exception e) {
            throw new ${capitalizedProject}Exception("Error ${action} ${entityName}: " + e.getMessage());
        }`;
    }

    

    static generateTryBlock(): string {
        return `        try {`;
    }

    

    static generateMethodWithReturn(
        signature: string,
        body: string,
        returnExpression: string,
        projectName: string,
        action: string,
        entityName: string
    ): string {
        return `    ${signature} {
        try {
${body}
            return ${returnExpression};
${this.generateCatchBlock(projectName, action, entityName)}
    }`;
    }

    

    static generateVoidMethod(
        signature: string,
        body: string,
        projectName: string,
        action: string,
        entityName: string
    ): string {
        return `    ${signature} {
        try {
${body}
${this.generateCatchBlock(projectName, action, entityName)}
    }`;
    }

    

    private static capitalize(str: string): string {
        if (!str) return '';
        return str.charAt(0).toUpperCase() + str.slice(1);
    }
}

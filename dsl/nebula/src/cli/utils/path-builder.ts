export class PathBuilder {
    private baseJavaPath: string;

    constructor(outputPath: string, basePackage: string, projectName: string) {
        const packagePath = basePackage.replace(/\./g, '/');
        this.baseJavaPath = `${outputPath}/src/main/java/${packagePath}/${projectName.toLowerCase()}`;
    }

    microservicePath(aggregateName: string, ...subPaths: string[]): string {
        return `${this.baseJavaPath}/microservices/${aggregateName.toLowerCase()}/${subPaths.join('/')}`;
    }

    entityPath(aggregateName: string, fileName: string): string {
        return this.microservicePath(aggregateName, 'aggregate', fileName);
    }

    servicePath(aggregateName: string, fileName: string): string {
        return this.microservicePath(aggregateName, 'service', fileName);
    }

    coordinationPath(aggregateName: string, ...subPaths: string[]): string {
        return this.microservicePath(aggregateName, 'coordination', ...subPaths);
    }

    eventsPath(aggregateName: string, ...subPaths: string[]): string {
        return this.microservicePath(aggregateName, 'events', ...subPaths);
    }

    sharedPath(...subPaths: string[]): string {
        return `${this.baseJavaPath}/shared/${subPaths.join('/')}`;
    }

    projectPath(...subPaths: string[]): string {
        return `${this.baseJavaPath}/${subPaths.join('/')}`;
    }

    getBaseJavaPath(): string {
        return this.baseJavaPath;
    }
}

export interface ServiceGenerationOptions {
    projectName: string;
}

export interface ServiceContext {
    aggregateName: string;
    capitalizedAggregate: string;
    packageName: string;
    rootEntity: any;
    projectName: string;
}

export interface ConfigsState {
    products: {[id: string]: Item};
    platforms: {[id: string]: Platform};
    taskVariantCategories: {[id: string]: TaskVariantCategory};
    tasks: {[id: string]: Task};
}

export interface Task extends Item {
    type: TaskType;
}

export interface Platform extends Item {
}

export interface TaskVariantCategory extends Item {
    type: TaskType;
    variants: Item[];
}

export interface Item {
    readonly id: string;
    readonly label: string;
}

export enum TaskType {
    BUILD = "BUILD",
    TEST = "TEST"
}

export interface JDKProject {
    readonly name: string;
    readonly url: string;
    readonly product: string;
    readonly jobConfig: JobConfig;
}

export interface JobConfig {
    readonly platforms: {[id: string]: PlatformConfig};
}

export interface PlatformConfig {
    readonly tasks: {[id: string]: TaskConfig};
}

export interface TaskConfig {
    readonly variants: VariantsConfig[];
}

export interface VariantsConfig {
    readonly map: {[key: string]: string};
    readonly platforms?: {[id: string]: PlatformConfig};
}

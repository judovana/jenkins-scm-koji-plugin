export interface Project {
    id: string;
    type: ProjectType;
}

export interface ProjectCategory {
    id: string;
    label: string;
    description: string;
    projectList?: Project[];
}

export interface JDKProject extends Project {
    url: string;
    readonly product: string;
    readonly jobConfiguration: JobConfig;
}

export interface Item {
    readonly id: string;
}

export enum TaskType {
    BUILD = "BUILD",
    TEST = "TEST"
}

export enum ProjectType {
    JDK_PROJECT = "JDK_PROJECT"
}


export interface JobConfig {
    platforms: { [id: string]: PlatformConfig };
}

export interface PlatformConfig {
    tasks: { [id: string]: TaskConfig };
}

export interface TaskConfig {
    variants: VariantsConfig[];
}

export interface VariantsConfig {
    map: { [key: string]: string };
    platforms?: { [id: string]: PlatformConfig };
}

export interface Task extends Item {
    type: TaskType;
}

export interface Platform extends Item {
}

export interface Product extends Item {
}

export interface TaskVariant extends Item {
    type: TaskType;
    variants: Item[];
}

export type ProjectCategories = { [id: string]: ProjectCategory };


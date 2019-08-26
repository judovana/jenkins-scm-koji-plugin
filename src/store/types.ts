export interface ConfigsState {
    products: {[id: string]: Item};
    platforms: {[id: string]: Platform};
    taskVariantCategories: {[id: string]: TaskVariantCategory};
    tasks: {[id: string]: Task};
}

export interface ProjectsState {
    projectCategories: {[id: string]: ProjectCategory};
    projects: {[id: string]: Project};
    selectedProjectCategoryId: string;
    selectedProjectId: string;
}

export interface ProjectCategory extends Item {
    description: string;
    list: string[];
}

export enum ProjectType {
    JDK_PROJECT = "JDK_PROJECT",
    JDK_TEST_PROJECT = "JDK_TEST_PROJECT",
    JAVA_PROJECT = "JAVA_PROJECT"
}

export interface Project {
    readonly name: string;
    readonly type: ProjectType
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

export interface JDKProject extends Project {
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

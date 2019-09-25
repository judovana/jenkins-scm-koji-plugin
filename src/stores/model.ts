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
    id: string;
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
    script: string;
    type: TaskType;
    machinePreference: "VM" | "VM_ONLY" | "HW" | "HW_ONLY";
    productLimitation: Limitation;
    platformLimitation: Limitation;
    fileRequirements: FileRequirements;
    xmlTemplate: string;
    rpmLimitaion: RPMLimitaion;
}

export interface FileRequirements {
    source: boolean;
    binary: "NONE" | "BINARY" | "BINARIES";
}

export type LimitFlag = "BLACKLIST" | "WHITELIST";

export interface Limitation {
    list: string[];
    flag: LimitFlag;
}

export interface RPMLimitaion {
    glob: string;
    flag: LimitFlag;
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


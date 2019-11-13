export interface Project extends Item {
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
    repoState?: RepoState;
    product: string;
    buildProviders: string[];
    jobConfiguration: JobConfig;
}

export interface Item {
    id: string;
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
    scmPollSchedule: string;
    machinePreference: MachinePreference;
    productLimitation: Limitation;
    platformLimitation: Limitation;
    fileRequirements: FileRequirements;
    xmlTemplate: string;
    rpmLimitation: RPMLimitation;
}

export type TaskType = "BUILD" | "TEST"

export type MachinePreference = "VM" | "VM_ONLY" | "HW" | "HW_ONLY"

export type BinaryRequirement = "NONE" | "BINARY" | "BINARIES"

export interface FileRequirements {
    source: boolean;
    binary: BinaryRequirement;
}

export type LimitFlag = "BLACKLIST" | "WHITELIST" | "NONE";

export interface Limitation {
    list: string[];
    flag: LimitFlag;
}

export interface RPMLimitation {
    glob: string;
    flag: LimitFlag;
}

export interface Platform extends Item {
    os: string
    version: string
    architecture: string
    provider: string
    vmName: string
    vmNodes: string[]
    hwNodes: string[]
    tags: string[]
}

export interface BuildProvider extends Item {
}

export interface Product extends Item {
}

export interface TaskVariant extends Item {
    type: TaskType;
    variants: Item[];
}

export interface FetchResult<T> {
    value?: T
    error?: string
}

export interface JobUpdateResult {
    jobName: string
    success: boolean
    message?: string
}

export interface JobUpdateResults {
    jobsCreated: JobUpdateResult[]
    jobsArchived: JobUpdateResult[]
    jobsRewritten: JobUpdateResult[]
    jobsRevived: JobUpdateResult[]
}

export interface OToolResponse {
    config?: Item
    jobUpdateResults: JobUpdateResults
}

export type ProjectCategories = { [id: string]: ProjectCategory };

export type ConfigState = "create" | "update"

export type ConfigGroup = {[id: string]: Item}

export type ConfigGroups = {[id: string]: ConfigGroup}

export type RepoState = "CLONED" | "NOT_CLONED" | "CLONE_ERROR" | "CLONING"

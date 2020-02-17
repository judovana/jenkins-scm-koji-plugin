export interface Project extends Item {
    buildProviders: string[]
    product: Product;
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
    jobConfiguration: JobConfig;
}

export interface Product {
    jdk: string
    packageName: string
}

export interface JDKTestProject extends Project {
    subpackageBlacklist: string[]
    subpackageWhitelist: string[]
    jobConfiguration: JobConfig
}

export type JDKTestProjectMap = { [id: string]: JDKTestProject }

export interface Item {
    id: string;
}

export type ProjectType = "JDK_PROJECT" | "JDK_TEST_PROJECT"


export interface JobConfig {
    platforms: PlatformConfig[]
}

export interface PlatformConfig {
    id: string
    tasks?: { [id: string]: TaskConfig }
    provider?: string
    variants?: VariantsConfig[]
}

export interface TaskConfig {
    variants: VariantsConfig[];
}

export interface VariantsConfig {
    map: { [key: string]: string };
    platforms?: PlatformConfig[]
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
    variables: Variable[]
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
    whitelist: string[]
    blacklist: string[]
}

export interface PlatformProvider extends Item {
    vmNodes: string[]
    hwNodes: string[]
}

export interface Platform extends Item {
    os: string
    version: string
    versionNumber: string
    architecture: string
    kojiArch?:string
    providers: PlatformProvider[]
    vmName: string
    tags: string[]
    variables: Variable[]
}

export interface BuildProvider extends Item {
}

export interface JDKVersion extends Item {
    packageNames: string[]
}

export interface TaskVariant extends Item {
    defaultValue: string
    supportsSubpackages: boolean
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

export type ConfigState = "new" | "edit"

export type ConfigMap = { [id: string]: Item }

export interface ConfigGroup extends Item {
    id: ConfigGroupId
    configs: ConfigMap
}

export type ConfigGroups = { [id in ConfigGroupId]: ConfigGroup }

export interface DisplayedConfig {
    groupId?: ConfigGroupId
    id?: string
}

export type RepoState = "CLONED" | "NOT_CLONED" | "CLONE_ERROR" | "CLONING"

export interface Variable {
    comment?: string
    commentedOut: boolean
    defaultPrefix: boolean
    exported: boolean
    name: string
    value: string
}

export type ConfigGroupId =
    | "buildProviders"
    | "platforms"
    | "jdkVersions"
    | "taskVariants"
    | "tasks"
    | "jdkProjects"
    | "jdkTestProjects"

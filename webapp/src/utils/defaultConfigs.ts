import {
    Platform,
    Task,
    JDKTestProject,
    JDKProject,
    ConfigGroupId,
    Item,
    Variable,
    PlatformProvider
} from "../stores/model"

const createDefaultJDKProject = (): JDKProject => ({
    buildProviders: [],
    id: "",
    jobConfiguration: { platforms: [] },
    product: {
        jdk: "",
        packageName: ""
    },
    type: "JDK_PROJECT",
    url: "",
    variables: []
})

const createDefaultJDKTestProject = (): JDKTestProject => ({
    buildProviders: [],
    id: "",
    jobConfiguration: { platforms: [] },
    product: {
        jdk: "",
        packageName: ""
    },
    subpackageBlacklist: [],
    subpackageWhitelist: [],
    type: "JDK_TEST_PROJECT",
    variables: []
})

const createDefaultPlatform = (): Platform => ({
    architecture: "",
    id: "",
    os: "",
    providers: [],
    tags: [],
    testingYstream: "NaN",
    stableZstream: "NaN",
    version: "",
    versionNumber: "",
    vmName: "",
    variables: []
})

const createDefaultTask = (): Task => ({
    fileRequirements: {
        binary: "NONE",
        noarch: false,
        source: false
    },
    id: "",
    machinePreference: "VM",
    platformLimitation: {
        flag: "NONE",
        list: []
    },
    productLimitation: {
        flag: "NONE",
        list: []
    },
    rpmLimitation: {
        blacklist: [],
        whitelist: []
    },
    scmPollSchedule: "",
    script: "",
    type: "TEST",
    xmlTemplate: "",
    xmlViewTemplate: "",
    timeoutInHours: "0",
    variables: []
})

export const createDefaultVariable = (): Variable => ({
    comment: "",
    commentedOut: false,
    defaultPrefix: true,
    exported: true,
    name: "",
    value: ""
})

export const createDefaultPlatfromProvider = (): PlatformProvider => ({
    hwNodes: [],
    id: "",
    vmNodes: []
})

const empty = (): Item => ({ id: "id" })

const defaults: { [id in ConfigGroupId]: () => Item } = {
    buildProviders: empty,
    jdkProjects: createDefaultJDKProject,
    jdkTestProjects: createDefaultJDKTestProject,
    jdkVersions: empty,
    platforms: createDefaultPlatform,
    tasks: createDefaultTask,
    taskVariants: empty
}

export default defaults

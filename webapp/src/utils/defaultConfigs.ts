import {
    Platform,
    Task,
    JDKTestProject,
    JDKProject,
    ConfigGroupId,
    Item
} from "../stores/model"

const createDefaultJDKProject = (): JDKProject => ({
    buildProviders: [],
    id: "",
    jobConfiguration: { platforms: {} },
    product: {
        jdk: "",
        packageName: ""
    },
    type: "JDK_PROJECT",
    url: ""
})

const createDefaultJDKTestProject = (): JDKTestProject => ({
    buildProviders: [],
    id: "",
    jobConfiguration: { platforms: {} },
    product: {
        jdk: "",
        packageName: ""
    },
    subpackageBlacklist: [],
    subpackageWhitelist: [],
    type: "JDK_TEST_PROJECT"
})

const createDefaultPlatform = (): Platform => ({
    architecture: "",
    id: "",
    os: "",
    providers: [],
    tags: [],
    version: "",
    versionNumber: "",
    vmName: "",
    variables: []
})

const createDefaultTask = (): Task => ({
    fileRequirements: {
        binary: "NONE",
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
    variables: []
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

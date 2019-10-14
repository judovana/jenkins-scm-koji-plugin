import { Task, JDKProject, ProjectType, PlatformConfig } from "./model";

const defaultTask: Task = {
    id: "",
    script: "",
    type: "TEST",
    scmPollSchedule: "",
    machinePreference: "VM",
    productLimitation: {
        flag: "NONE",
        list: []
    },
    platformLimitation: {
        flag: "NONE",
        list: []
    },
    fileRequirements: {
        source: false,
        binary: "NONE"
    },
    xmlTemplate: "",
    rpmLimitation: {
        flag: "NONE",
        glob: ""
    }
}

const defaultJDKProject: JDKProject = {
    id: "",
    type: ProjectType.JDK_PROJECT,
    url: "",
    buildProviders: [],
    product: "",
    jobConfiguration: {
        platforms: {} as { [id: string]: PlatformConfig }
    }
}

export { defaultTask, defaultJDKProject }

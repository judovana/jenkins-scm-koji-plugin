import { Task } from "./model";

const defaultTask: Task = {
    id: "",
    script: "",
    type: "TEST",
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

export { defaultTask }

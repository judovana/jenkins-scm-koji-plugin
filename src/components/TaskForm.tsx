import React from "react";
import { observable } from "mobx";
import { observer, inject } from "mobx-react";
import { CONFIG_STORE, ConfigStore } from "../stores/ConfigStore";
import { Task, TaskType, MachinePreference, BinaryRequirement, LimitFlag, RPMLimitation } from "../stores/model";
import LimitationForm from "./formComponents/LimitationForm";
import TextInput from "./formComponents/TextInput";
import TextArea from "./formComponents/TextArea";
import Checkbox from "./formComponents/Checkbox";
import Select from "./formComponents/Select";

interface Props {
    task?: Task;
    configStore?: ConfigStore;
}

class TaskForm extends React.PureComponent<Props> {

    @observable
    task: Task;

    constructor(props: Props) {
        super(props)
        this.task = props.task || defaultTask
    }

    onIdChange = (value: string) => {
        this.task.id = value
    }

    onTypeChange = (value: string) => {
        this.task.type = value as TaskType
    }

    onMachinePreferenceChange = (value: string) => {
        this.task.machinePreference = value as MachinePreference
    }

    onSourcesChange = (value: boolean) => {
        this.task.fileRequirements.source = value
    }

    onBinaryChange = (value: string) => {
        this.task.fileRequirements.binary = value as BinaryRequirement
    }

    onXmlTemplateChange = (value: string) => {
        this.task.xmlTemplate = value
    }

    onRPMLimitationFlagChange = (value: string) => {
        this.task.rpmLimitation.flag = value as LimitFlag
    }

    onRPMLimitationGlobChange = (value: string) => {
        this.task.rpmLimitation.glob = value
    }

    renderRPMLimitaion = (rpmLimitation: RPMLimitation) => {
        let flag: LimitFlag
        let glob: string
        if (!rpmLimitation) {
            flag = "NONE"
            glob = ""
        } else {
            flag = rpmLimitation.flag || "NONE"
            glob = rpmLimitation.glob || ""
        }
        return (
            <div className="field-container">
                <div className="label-container">RPM limitation</div>
                <div className="value-container">
                    <Select
                        onChange={this.onRPMLimitationFlagChange}
                        options={["NONE", "WHITELIST", "BLACKLIST"]}
                        value={flag} />
                    {
                        flag !== "NONE" &&
                        <TextInput
                            label={"glob"}
                            onChange={this.onRPMLimitationGlobChange}
                            placeholder={"Enter glob"}
                            value={glob} />
                    }
                </div>
            </div>
        )
    }

    render() {
        const configStore = this.props.configStore!;
        const { id, platformLimitation, productLimitation } = this.task;
        return (
            <fieldset>
                <TextInput
                    label={"Task id"}
                    onChange={this.onIdChange}
                    value={id} />
                <Select
                    label={"type"}
                    onChange={this.onTypeChange}
                    options={["BUILD", "TEST"]}
                    value={this.task.type} />
                <Select
                    label={"machine preference"}
                    onChange={this.onMachinePreferenceChange}
                    options={["VM", "VM_ONLY", "HW", "HW_ONLY"]}
                    value={this.task.machinePreference} />
                <TextInput
                    label={"script"}
                    value={this.task.script}
                    onChange={(value) => { this.task.script = value }}
                    placeholder={"Enter path to bash script"} />
                <LimitationForm
                    label={"platform limitations"}
                    limitation={platformLimitation}
                    items={configStore.platforms} />
                <LimitationForm
                    label={"product limitations"}
                    limitation={productLimitation}
                    items={configStore.products} />
                <Checkbox
                    label="require sources"
                    value={this.task.fileRequirements.source}
                    onChange={this.onSourcesChange} />
                <Select
                    label={"binary requirements"}
                    onChange={this.onBinaryChange}
                    options={["NONE", "BINARY", "BINARIES"]}
                    value={this.task.fileRequirements.binary} />
                <TextArea
                    label={"xml template"}
                    onChange={this.onXmlTemplateChange}
                    placeholder={"Enter xml template for post build tasks"}
                    value={this.task.xmlTemplate} />
                {this.renderRPMLimitaion(this.task.rpmLimitation)}
                <br />
                <br />
                <br />
                {JSON.stringify(this.task)}
            </fieldset>
        );
    }
}

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

export default inject(CONFIG_STORE)(observer(TaskForm));

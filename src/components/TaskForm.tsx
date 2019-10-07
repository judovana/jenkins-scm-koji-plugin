import React from "react";
import { observable, runInAction } from "mobx";
import { observer, inject } from "mobx-react";
import { CONFIG_STORE, ConfigStore } from "../stores/ConfigStore";
import { Task, TaskType, MachinePreference, BinaryRequirement, LimitFlag, RPMLimitation, FileRequirements } from "../stores/model";
import LimitationForm from "./formComponents/LimitationForm";
import TextInput from "./formComponents/TextInput";
import TextArea from "./formComponents/TextArea";
import Checkbox from "./formComponents/Checkbox";
import Select from "./formComponents/Select";

type TaskFormProps = {
    task: Task
    configStore?: ConfigStore;
}

class TaskForm extends React.PureComponent<TaskFormProps> {

    @observable
    task?: Task;

    componentDidMount() {
        const task = this.props.task
        this.task = { ...task }
    }

    componentDidUpdate() {
        const task = this.props.task
        if (task.id === "") {
            return
        }
        if (this.task!.id !== task.id) {
            runInAction(() => {
                this.task = { ...task }
            })
        }
    }

    onIdChange = (value: string) => {
        this.task!.id = value
    }

    onTypeChange = (value: string) => {
        this.task!.type = value as TaskType
    }

    onMachinePreferenceChange = (value: string) => {
        this.task!.machinePreference = value as MachinePreference
    }

    onSourcesChange = (value: boolean) => {
        this.task!.fileRequirements.source = value
    }

    onScriptChange = (value: string) => {
        this.task!.script = value
    }

    onBinaryChange = (value: string) => {
        this.task!.fileRequirements.binary = value as BinaryRequirement
    }

    onXmlTemplateChange = (value: string) => {
        this.task!.xmlTemplate = value
    }

    onRPMLimitationFlagChange = (value: string) => {
        this.task!.rpmLimitation.flag = value as LimitFlag
    }

    onRPMLimitationGlobChange = (value: string) => {
        this.task!.rpmLimitation.glob = value
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

    renderFileRequirementsForm = (fileRequirements: FileRequirements) => {
        let requireSources = false
        let binaryRequirement: BinaryRequirement = "NONE"
        if (fileRequirements) {
            requireSources = fileRequirements.source || false
            binaryRequirement = fileRequirements.binary || "NONE"
        }
        return (
            <div className="field-container">
                <div className="label-container">file requirements</div>
                <div className="value-container">
                    <Checkbox
                        label="require sources"
                        onChange={this.onSourcesChange}
                        value={requireSources} />
                    <Select
                        label={"binary requirements"}
                        onChange={this.onBinaryChange}
                        options={["NONE", "BINARY", "BINARIES"]}
                        value={binaryRequirement} />
                </div>
            </div>
        )
    }

    render() {
        const configStore = this.props.configStore!;
        if (!this.task) {
            return null
        }
        const { id, fileRequirements, platformLimitation, productLimitation } = this.task;
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
                    onChange={this.onScriptChange}
                    placeholder={"Enter path to bash script"} />
                <LimitationForm
                    label={"platform limitations"}
                    limitation={platformLimitation}
                    items={configStore.platforms} />
                <LimitationForm
                    label={"product limitations"}
                    limitation={productLimitation}
                    items={configStore.products} />
                {this.renderFileRequirementsForm(fileRequirements)}
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

export default inject(CONFIG_STORE)(observer(TaskForm));

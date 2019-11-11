import React from "react";
import { observable, runInAction } from "mobx";
import { observer, inject } from "mobx-react";
import { CONFIG_STORE, ConfigStore } from "../../stores/ConfigStore";
import { Task, TaskType, MachinePreference, BinaryRequirement, LimitFlag, RPMLimitation, FileRequirements, ConfigState } from "../../stores/model";
import LimitationForm from "../formComponents/LimitationForm";
import TextInput from "../formComponents/TextInput";
import TextArea from "../formComponents/TextArea";
import Checkbox from "../formComponents/Checkbox";
import Select from "../formComponents/Select";
import Button from "../Button";
import RPMLimitationForm from "../formComponents/RPMLimitationForm";
import FileRequirementsForm from "../formComponents/FileRequirementsForm";

type TaskFormProps = {
    task: Task
    configStore?: ConfigStore;
}

class TaskForm extends React.PureComponent<TaskFormProps> {

    @observable
    task?: Task;

    @observable
    taskState?: ConfigState

    componentDidMount() {
        const task = this.props.task
        this.taskState = task.id === "" ? "create" : "update"
        this.task = { ...task }
    }

    componentDidUpdate() {
        const task = this.props.task
        const state = this.props.configStore!.configState
        if (state !== this.taskState) {
            runInAction(() => {
                this.task = { ...task }
                this.taskState = state
            })
            return
        }
        if (state === "update" && this.task!.id !== task.id) {
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

    onSCMPollScheduleChange = (value: string) => {
        this.task!.scmPollSchedule = value
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

    onSubmit = () => {
        const configStore = this.props.configStore!
        switch (this.taskState) {
            case "create":
                configStore.createConfig(this.task!)
                break
            case "update":
                configStore.updateConfig(this.task!)
                break;
        }
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
                        options={["WHITELIST", "BLACKLIST"]}
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
        const configState = configStore.configState
        const { id, fileRequirements, platformLimitation, productLimitation, rpmLimitation } = this.task;
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
                    label={"SCM poll schedule"}
                    onChange={this.onSCMPollScheduleChange}
                    value={this.task.scmPollSchedule}>
                </TextInput>
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
                <FileRequirementsForm
                    fileRequirements={fileRequirements}
                    onBinaryChange={this.onBinaryChange}
                    onSourcesChange={this.onSourcesChange}/>
                <TextArea
                    label={"xml template"}
                    onChange={this.onXmlTemplateChange}
                    placeholder={"Enter xml template for post build tasks"}
                    value={this.task.xmlTemplate} />
                <RPMLimitationForm
                    rpmLimitation={rpmLimitation}
                    onFlagChange={this.onRPMLimitationFlagChange}
                    onGlobChange={this.onRPMLimitationGlobChange}/>
                <Button onClick={this.onSubmit}>{configState}</Button>
                <br />
                <br />
                <br />
                {JSON.stringify(this.task)}
            </fieldset>
        );
    }
}

export default inject(CONFIG_STORE)(observer(TaskForm));

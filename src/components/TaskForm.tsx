import React from "react";
import { observable } from "mobx";
import { observer, inject } from "mobx-react";
import { CONFIG_STORE, ConfigStore } from "../stores/ConfigStore";
import { Task, TaskType, FileRequirements } from "../stores/model";
import Dropdown from "./formComponents/Dropdown";
import LimitationForm from "./formComponents/LimitationForm";
import TextInput from "./formComponents/TextInput";
import TextArea from "./formComponents/TextArea";
import Checkbox from "./formComponents/Checkbox";

interface Props {
    task: Task;
    configStore?: ConfigStore;
}

class TaskForm extends React.PureComponent<Props> {

    @observable
    task: Task;

    constructor(props: Props) {
        super(props);
        this.task = props.task;
    }

    componentDidUpdate() {
        if (this.task.id !== this.props.task.id) {
            this.task = this.props.task;
        }
    }

    render() {
        const configStore = this.props.configStore!;
        const { id, platformLimitation, productLimitation } = this.task;
        return (
            <div>
                <TextInput
                    label={"Task id"}
                    onChange={(value) => this.task.id = value}
                    value={id} />
                <div style={{ height: 20 }} />
                <Dropdown
                    onChange={(value => this.task.type = value as TaskType)}
                    value={this.task.type}
                    values={[{ id: "BUILD" as TaskType }, { id: "TEST" as TaskType }]}
                    label={"type"} />
                <div style={{ height: 20 }} />
                <Dropdown
                    onChange={(value => this.task.machinePreference = value as "VM" | "VM_ONLY" | "HW" | "HW_ONLY")}
                    value={this.task.machinePreference}
                    values={[{ id: "VM" }, { id: "VM_ONLY" }, { id: "HW" }, { id: "HW_ONLY" }]}
                    label={"machine preference"} />
                <div style={{ height: 20 }} />
                <TextArea
                    label={"script"}
                    value={this.task.script}
                    onChange={(value) => { this.task.script = value }}
                    placeholder={"Enter bash script"} />
                <div style={{ height: 20 }} />
                <LimitationForm
                    label={"platform limitations"}
                    limitation={platformLimitation}
                    items={configStore.platforms} />
                <div style={{ height: 20 }} />
                <LimitationForm
                    label={"product limitations"}
                    limitation={productLimitation}
                    items={configStore.products} />
                <div style={{ height: 20 }} />
                <Checkbox
                    label="require sources"
                    value={this.task.fileRequirements.source}
                    onChange={(value) => this.task.fileRequirements.source = value} />
                <Dropdown
                    onChange={(value) => this.task.fileRequirements.binary = value as "NONE" | "BINARY" | "BINARIES"}
                    values={[{ id: "NONE" }, { id: "BINARY" }, { id: "BINARIES" }]}
                    value={this.task.fileRequirements.binary}
                    label={"binary requirements"} />
                <br />
                <br />
                <br />
                {JSON.stringify(this.task)}
            </div>
        );
    }
}

export default inject(CONFIG_STORE)(observer(TaskForm));

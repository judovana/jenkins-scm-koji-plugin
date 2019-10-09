import React from "react";

import { observer, inject } from "mobx-react";

import { JDKProject, ConfigState } from "../stores/model";
import { CONFIG_STORE, ConfigStore } from "../stores/ConfigStore";
import { observable, runInAction } from "mobx";
import JobConfigComponent from "./JobConfigComponent";
import TextInput from "./formComponents/TextInput";
import Select from "./formComponents/Select";
import Button from "./Button";
import MultiSelect from "./formComponents/MultiSelect";

interface Props {
    project: JDKProject;
    configStore?: ConfigStore;
}

class JDKProjectForm extends React.PureComponent<Props> {

    @observable
    private jdkProject?: JDKProject;

    @observable
    private jdkProjectState?: ConfigState

    componentDidMount() {
        const jdkProject = this.props.project
        this.jdkProjectState = jdkProject.id === "" ? "create" : "update"
        this.jdkProject = { ...jdkProject }
    }

    componentDidUpdate() {
        const task = this.props.project
        const state = this.props.configStore!.configState
        if (state !== this.jdkProjectState) {
            runInAction(() => {
                this.jdkProject = { ...task }
                this.jdkProjectState = state
            })
            return
        }
        if (state === "update" && this.jdkProject!.id !== task.id) {
            runInAction(() => {
                this.jdkProject = { ...task }
            })
        }
    }

    onIdChange = (value: string) => {
        this.jdkProject!.id = value
    }

    onBuildProvidersChange = (values: string[]) => {
        this.jdkProject!.buildProviders = values
    }

    onUrlChange = (value: string) => {
        this.jdkProject!.url = value
    }

    onProductChange = (value: string) => {
        this.jdkProject!.product = value
    }

    onSubmit = () => {
        const configStore = this.props.configStore!
        switch (this.jdkProjectState) {
            case "create":
                configStore.postConfig(this.jdkProject!)
                break
            case "update":
                configStore.putConfig(this.jdkProject!)
                break;
        }
    }

    renderBuildProvidersForm = () => {
        const buildProviders = this.props.configStore!.buildProviders
        return (
            <MultiSelect
                label={"build providers"}
                onChange={this.onBuildProvidersChange}
                options={buildProviders.map(buildProvider => buildProvider.id)}
                values={this.jdkProject!.buildProviders}
                />
        )
    }

    render() {
        const configStore = this.props.configStore!
        const jdkProject = this.jdkProject
        if (!jdkProject) {
            return null
        }
        const configState = configStore.configState
        const products = configStore.products
        return (
            <fieldset>
                <TextInput
                    label={"id"}
                    value={jdkProject!.id}
                    onChange={this.onIdChange} />
                <TextInput
                    label={"url"}
                    value={jdkProject!.url}
                    onChange={this.onUrlChange} />
                {this.renderBuildProvidersForm()}
                <Select
                    label={"Product"}
                    options={products.map(product => product.id)}
                    value={jdkProject!.product}
                    onChange={this.onProductChange} />
                <JobConfigComponent jobConfig={jdkProject!.jobConfiguration} />
                <Button onClick={this.onSubmit}>{configState}</Button>
                <br />
                <br />
                <br />
                <p>{JSON.stringify(jdkProject)}</p>
            </fieldset>
        )
    }
}

export default inject(CONFIG_STORE)(observer(JDKProjectForm));

import React from "react";

import { observer, inject } from "mobx-react";

import { JDKProject } from "../stores/model";
import { CONFIG_STORE, ConfigStore } from "../stores/ConfigStore";
import { observable, runInAction } from "mobx";
import JobConfigComponent from "./JobConfigComponent";
import TextInput from "./formComponents/TextInput";
import Select from "./formComponents/Select";

interface Props {
    project: JDKProject;
    configStore?: ConfigStore;
}

class JDKProjectForm extends React.PureComponent<Props> {

    @observable
    private jdkProject?: JDKProject;

    componentDidMount() {
        const jdkProject = this.props.project
        this.jdkProject = { ...jdkProject }
    }

    componentDidUpdate() {
        const task = this.props.project
        if (task.id === "") {
            return
        }
        if (this.jdkProject!.id !== task.id) {
            runInAction(() => {
                this.jdkProject = { ...task }
            })
        }
    }

    onIdChange = (value: string) => {
        this.jdkProject!.id = value
    }

    onUrlChange = (value: string) => {
        this.jdkProject!.url = value
    }

    onProductChange = (value: string) => {
        this.jdkProject!.product = value
    }

    render() {
        const configStore = this.props.configStore!
        const jdkProject = this.jdkProject
        if (jdkProject) {
            return null
        }
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
                <Select
                    label={"Product"}
                    options={products.map(product => product.id)}
                    value={jdkProject!.product}
                    onChange={(value: string) => this.setState({ product: value })} />
                <JobConfigComponent jobConfig={jdkProject!.jobConfiguration} />
                <br />
                <br />
                <br />
                <p>{JSON.stringify(jdkProject)}</p>
            </fieldset>
        )
    }
}

export default inject(CONFIG_STORE)(observer(JDKProjectForm));

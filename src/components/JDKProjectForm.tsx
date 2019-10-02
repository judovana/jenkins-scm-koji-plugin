import React from "react";

import { observer, inject } from "mobx-react";

import { JDKProject, PlatformConfig, ProjectType } from "../stores/model";
import { CONFIG_STORE, ConfigStore } from "../stores/ConfigStore";
import { observable } from "mobx";
import JobConfigComponent from "./JobConfigComponent";
import TextInput from "./formComponents/TextInput";
import Select from "./formComponents/Select";

interface Props {
    project?: JDKProject;
    configStore?: ConfigStore;
}

class JDKProjectForm extends React.PureComponent<Props> {

    @observable
    private jdkProject: JDKProject;

    constructor(props: Props) {
        super(props);
        this.jdkProject = props.project ? props.project : defaultProject
    }

    onIdChange = (value: string) => {
        this.jdkProject.id = value
    }

    onUrlChange = (value: string) => {
        this.jdkProject.url = value
    }

    onProductChange = (value: string) => {
        this.jdkProject.product = value
    }

    render() {
        const configStore = this.props.configStore!
        const project = this.jdkProject
        const products = configStore.products
        return (
            <fieldset>
                <TextInput
                    label={"id"}
                    value={project.id}
                    onChange={this.onIdChange} />
                <TextInput
                    label={"url"}
                    value={project.url}
                    onChange={this.onUrlChange} />
                <Select
                    label={"Product"}
                    options={products.map(product => product.id)}
                    value={project.product}
                    onChange={(value: string) => this.setState({ product: value })} />
                <JobConfigComponent jobConfig={project.jobConfiguration} />
                <br />
                <br />
                <br />
                <p>{JSON.stringify(project)}</p>
            </fieldset>
        )
    }
}

const defaultProject: JDKProject = {
    id: "",
    type: ProjectType.JDK_PROJECT,
    url: "",
    product: "",
    jobConfiguration: {
        platforms: {} as { [id: string]: PlatformConfig }
    }
}

export default inject(CONFIG_STORE)(observer(JDKProjectForm));

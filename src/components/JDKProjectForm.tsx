import React from "react";

import { observer, inject } from "mobx-react";

import Dropdown from "./Dropdown";
import JobConfigComponent from "./JobConfigComponent";

import { JDKProject, JobConfig, PlatformConfig } from "../stores/model";
import { CONFIG_STORE, ConfigStore } from "../stores/ConfigStore";

interface Props {
    project?: JDKProject;
    configStore?: ConfigStore;
}

class JDKProjectForm extends React.PureComponent<Props, JDKProject> {

    constructor(props: Props) {
        super(props);
        this.state = props.project ? props.project :
            {
                id: "",
                url: "",
                product: "",
                jobConfiguration: {
                    platforms: {} as { [id: string]: PlatformConfig }
                }
            } as JDKProject;
    }

    handleNameChange = (event: React.ChangeEvent<HTMLInputElement>): void => {
        this.setState({ id: event.currentTarget.value });
    }

    handleUrlChange = (event: React.ChangeEvent<HTMLInputElement>): void => {
        this.setState({ url: event.currentTarget.value });
    }

    handleJobConfigChange = (config: JobConfig): void => {
        this.setState({
            jobConfiguration: config
        });
    }

    render() {
        const configStore = this.props.configStore;
        if (!configStore) {
            return null;
        }
        const products = Array.from(configStore.products.values());
        return (this.props.configStore &&
            <div>
                <div>
                    <label>name: </label>
                    <input
                        type="text"
                        value={this.state.id}
                        onChange={this.handleNameChange} />
                </div>
                <div>
                    <label>url: </label>
                    <input
                        type="text"
                        value={this.state.url}
                        onChange={this.handleUrlChange} />
                </div>
                <Dropdown
                    label={"Product"}
                    values={products}
                    value={this.state.product}
                    onChange={(value: string) => this.setState({ product: value })} />
                <JobConfigComponent
                    onChange={this.handleJobConfigChange}
                    jobConfig={this.state.jobConfiguration} />
                <br />
                <br />
                <br />
                <p>{JSON.stringify(this.state)}</p>
            </div>
        )
    }
}

export default inject(CONFIG_STORE)(observer(JDKProjectForm));

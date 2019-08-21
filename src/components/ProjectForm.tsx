import React from "react";
import { connect } from "react-redux";

import Dropdown from "./Dropdown";
import JobConfigComponent from "./JobConfigComponent";

import { AppState } from "../store/reducer";
import { Item, Project, JobConfig } from "../store/types";

interface Props {
    project?: Project;
}

interface StateProps {
    products: Item[];
}

interface State {
    name: string;
    url: string;
    product: string;
    jobConfig: JobConfig;
}

class ProjectForm extends React.PureComponent<Props & StateProps, State> {

    constructor(props: Props & StateProps) {
        super(props);
        this.state = props.project ? props.project :
        {
            name: "",
            url: "",
            product: "",
            jobConfig: {
                platforms: {}
            }
        };
    }

    handleNameChange = (event: React.ChangeEvent<HTMLInputElement>): void => {
        this.setState({name: event.currentTarget.value});
    }

    handleUrlChange = (event: React.ChangeEvent<HTMLInputElement>): void => {
        this.setState({url: event.currentTarget.value});
    }

    handleJobConfigChange = (config: JobConfig): void => {
        this.setState({
            jobConfig: config
        });
    }

    render() {
        return(
            <div>
                <div>
                    <label>name: </label>
                    <input
                        type="text"
                        value={this.state.name}
                        onChange={this.handleNameChange}/>
                </div>
                <div>
                    <label>url: </label>
                    <input
                        type="text"
                        value={this.state.url}
                        onChange={this.handleUrlChange}/>
                </div>
                <Dropdown
                    label={"Product"}
                    values={this.props.products}
                    value={this.state.product}
                    onChange={(value: string) => this.setState({product: value})}/>
                <JobConfigComponent
                    onChange={this.handleJobConfigChange}
                    jobConfig={this.state.jobConfig}/>
                <br/>
                <br/>
                <br/>
                <p>{JSON.stringify(this.state)}</p>
            </div>
        )
    }
}

const mapStateToProps = (state: AppState): StateProps => ({
    products: Object.values(state.configs.products)
});

export default connect(mapStateToProps)(ProjectForm);

import React from "react";
import { inject, observer } from "mobx-react"

import { ConfigStore, CONFIG_STORE } from "../../stores/ConfigStore"
import { JDKProject, Task, Platform } from "../../stores/model"
import JDKProjectForm from "./JDKProjectForm";
import TaskForm from "./TaskForm";
import PlatformForm from "./PlatformForm";

import Button from "../Button";

interface Props {
    configStore?: ConfigStore
}

class ConfigForm extends React.PureComponent<Props> {

    renderError = () => {
        const { discardError, errorMessage } = this.props.configStore!
        return (
            errorMessage &&
            <div className="error-container">
                <div>{errorMessage}</div>
                <Button onClick={discardError} color="red">discard</Button>
            </div>
        )
    }

    renderForm = () => {
        const { selectedConfig, selectedGroupId } = this.props.configStore!;
        if (!selectedConfig) {
            return
        }
        switch (selectedGroupId) {
            case "jdkProjects":
                return (
                    <JDKProjectForm project={selectedConfig as JDKProject} />
                );
            case "tasks":
                return (
                    <TaskForm task={selectedConfig as Task} />
                );
            case "platforms":
                return (
                    <PlatformForm platform={selectedConfig as Platform} />
                )
            default:
                return null;
        }
    }

    render() {
        return (
            <div>
                {this.renderError()}
                <div className="form-container">
                    <form>
                        {this.renderForm()}
                    </form>
                </div>
            </div>
        )
    }
}

export default inject(CONFIG_STORE)(observer(ConfigForm));

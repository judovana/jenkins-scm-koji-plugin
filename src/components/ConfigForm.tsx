import React from "react";

import { JDKProject, Item, Task } from "../stores/model";
import JDKProjectForm from "./JDKProjectForm";
import TaskForm from "./TaskForm";

import "../styles/Forms.css";

interface Props {
    group: string;
    config: Item;
}

class ConfigForm extends React.PureComponent<Props> {

    renderForm = () => {
        const { config, group } = this.props;
        switch (group) {
            case "jdkProjects":
                return (
                    <JDKProjectForm project={config as JDKProject} />
                );
            case "tasks":
                return (
                    <TaskForm task={config as Task} />
                );
            default:
                return null;
        }
    }

    render() {
        return (
            <form>
                {this.renderForm()}
            </form>
        )
    }
}

export default ConfigForm;

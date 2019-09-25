import React from "react";

import { JDKProject, Item, Task } from "../stores/model";
import JDKProjectForm from "./JDKProjectForm";
import TaskForm from "./TaskForm";

interface Props {
    group: string;
    config: Item;
}

class ConfigForm extends React.PureComponent<Props> {

    render() {
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
}

export default ConfigForm;

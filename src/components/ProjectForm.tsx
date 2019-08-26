import React from "react";
import { Project, ProjectType, JDKProject } from "../store/types";
import JDKProjectForm from "./JDKProjectForm";

interface Props {
    project: Project;
}

class ProjectForm extends React.PureComponent<Props> {

    render() {
        const { project } = this.props;
        switch (this.props.project.type) {
            case ProjectType.JDK_PROJECT:
                return (
                    <JDKProjectForm project={project as JDKProject} />
                )
        }
    }
}

export default ProjectForm;

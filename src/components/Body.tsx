import React from "react";

import { inject, observer } from "mobx-react";
import { ProjectStore, PROJECT_STORE } from "../stores/ProjectStore";
import ProjectForm from "./ProjectForm";
import { CONFIG_STORE, ConfigStore } from "../stores/ConfigStore";

interface Props {
    projectStore?: ProjectStore;
    configStore?: ConfigStore;
}

class Body extends React.PureComponent<Props> {

    componentDidMount() {
        const { configStore, projectStore } = this.props;

        if (configStore) {
            configStore.fetchPlatforms();
            configStore.fetchProducts();
            configStore.fetchTasks();
            configStore.fetchTaskVariants();
        }
        if (projectStore) {
            projectStore.fetchJDKProjects();
        }
    }

    render() {
        const { projectStore } = this.props;

        if (!projectStore) {
            return <div>no store</div>;
        }
        const projectCategory = projectStore.selectedProjectCategory;
        if (!projectCategory) {
            return <div></div>
        }
        const projects = projectStore.projects;
        const project = projectStore.selectedProject;
        return (
            <div className="Body">
                {
                    projectCategory &&
                    <div className="List">
                        <div className="ListHeader">
                            {projectCategory.label}
                        </div>
                        <div>
                            {

                                Array.from(projects.values()).map(project =>
                                    <div
                                        className="ListItem"
                                        onClick={() => projectStore.selectedProjectId = project.id}
                                        key={project.id}>
                                        {project.id}
                                    </div>
                                )
                            }
                        </div>
                    </div>
                }
                {
                    project &&
                    <div>
                        <ProjectForm project={project} />
                    </div>
                }
            </div>
        )
    }
}

export default inject(PROJECT_STORE, CONFIG_STORE)(observer(Body));

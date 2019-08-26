import React from "react";
import { ProjectCategory, Project } from "../store/types";
import { connect } from "react-redux";
import { AppState } from "../store/reducer";

import { Dispatch, Action } from "redux";
import { selectProject } from "../store/projects/actions";
import ProjectForm from "./ProjectForm";

interface StateProps {
    projectCategory: ProjectCategory;
    project: Project;
}

interface DispatchProps {
    selectProject: (id: string) => void
}

class Body extends React.PureComponent<StateProps & DispatchProps> {

    render() {
        const { projectCategory, project } = this.props;

        if (!projectCategory) {
            return null;
        }
        return (
            <div className="Body">
                <div className="List">
                    <div className="ListHeader">
                        Projects
                    </div>
                    {
                        projectCategory.list.map(projectId =>
                            <div className="ListItem" key={projectId} onClick={() => this.props.selectProject(projectId)}>
                                {projectId}
                            </div>
                        )
                    }
                </div>
                {
                    !project ? null :
                        <div className="Content">
                            <ProjectForm project={project} />
                        </div>
                }
            </div>
        )
    }
}

const mapStateToProps = (state: AppState): StateProps => ({
    projectCategory: state.projects.projectCategories[state.projects.selectedProjectCategoryId],
    project: state.projects.projects[state.projects.selectedProjectId]
});

const mapDispatchToProps = (dispatch: Dispatch<Action>): DispatchProps => ({
    selectProject: id => dispatch(selectProject(id))
});

export default connect(mapStateToProps, mapDispatchToProps)(Body);

import React from "react";

import { AppState } from "../store/reducer";
import { connect } from "react-redux";
import { Dispatch, Action } from "redux";
import { ProjectCategory } from "../store/types";
import { selectProjectCategory } from "../store/projects/actions";

interface StateProps {
    projects: ProjectCategory[];
}

interface DispatchProps {
    selectProject: (id: string) => void
}

class Header extends React.PureComponent<StateProps & DispatchProps> {

    render() {
        return (
            <div style={{
                display: "flex",
                flexDirection: "row"
            }}>
                {
                    this.props.projects.map(project =>
                        <div className="HeaderItem" key={project.id} onClick={() => this.props.selectProject(project.id)}>
                            <div style={{ textAlign: "center", fontSize: 20 }}>{project.label}</div>
                            <div style={{ textAlign: "center" }}>{project.description}</div>
                        </div>
                    )
                }
            </div>
        )
    }
}

const mapStateToProps = (state: AppState): StateProps => ({
    projects: Object.values(state.projects.projectCategories)
})

const mapDispatchToProps = (dispatch: Dispatch<Action>): DispatchProps => ({
    selectProject: (id: string): void => { dispatch(selectProjectCategory(id)) }
})

export default connect(mapStateToProps, mapDispatchToProps)(Header);

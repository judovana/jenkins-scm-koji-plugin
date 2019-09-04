import React from "react";
import { observer, inject } from "mobx-react";

import { ProjectStore, PROJECT_STORE } from "../stores/ProjectStore";

interface Props {
    projectStore?: ProjectStore;
}

class Header extends React.PureComponent<Props> {

    render() {
        const { projectStore } = this.props;
        return (projectStore &&
            <div style={{
                display: "flex",
                flexDirection: "row"
            }}>
                {
                    Object.values(projectStore.projectCategories).map(category =>
                        <div className="HeaderItem" key={category.id} onClick={() => { projectStore.selectedProjectCategoryId = category.id }}>
                            <div style={{ textAlign: "center", fontSize: 20 }}>{category.label}</div>
                            <div style={{ textAlign: "center" }}>{category.description}</div>
                        </div>
                    )
                }
            </div>
        );
    }
}

export default inject(PROJECT_STORE)(observer(Header));

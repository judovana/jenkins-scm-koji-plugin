import { ProjectsState } from "../types";
import { actionTypes, SelectProjectCategoryAction, SelectProjectAction } from "./actions";

const reducer = (state: ProjectsState = initialState, action: any): ProjectsState => {
    switch (action.type) {
        case actionTypes.SELECT_PROJECT_CATEGORY:
            return setSelectedProjectCategory(state, action as SelectProjectCategoryAction);
        case actionTypes.SELECT_PROJECT:
            return setSelectedProject(state, action as SelectProjectAction);
        default:
            return state;
    }
};

const setSelectedProjectCategory = (state: ProjectsState, action: SelectProjectCategoryAction): ProjectsState => ({
    ...state,
    selectedProjectCategoryId: action.payload
});

const setSelectedProject = (state: ProjectsState, action: SelectProjectAction): ProjectsState => ({
    ...state,
    selectedProjectId: action.payload
})

const initialState: ProjectsState = {
    projectCategories: {
    },
    projects: {
    },
    selectedProjectCategoryId: "",
    selectedProjectId: ""
};

export default reducer;

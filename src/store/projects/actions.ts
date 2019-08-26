import { Action } from "redux";

export const actionTypes = {
    SELECT_PROJECT_CATEGORY: "SELECT_PROJECT_TYPE",
    SELECT_PROJECT: "SELECT_PROJECT"
}

export interface SelectProjectCategoryAction extends Action {
    type: typeof actionTypes.SELECT_PROJECT_CATEGORY;
    payload: string;
}

export interface SelectProjectAction extends Action {
    type: typeof actionTypes.SELECT_PROJECT_CATEGORY;
    payload: string;
}

export const selectProjectCategory = (id: string): SelectProjectCategoryAction => ({
    type: actionTypes.SELECT_PROJECT_CATEGORY,
    payload: id
});

export const selectProject = (id: string): SelectProjectAction => ({
    type: actionTypes.SELECT_PROJECT,
    payload: id
});

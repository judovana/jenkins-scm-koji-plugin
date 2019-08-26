import { combineReducers } from "redux";

import configs from "./configs/reducer";
import projects from "./projects/reducer";

const rootReducer = combineReducers({
    configs: configs,
    projects: projects
});

export default rootReducer;

export type AppState = ReturnType<typeof rootReducer>;

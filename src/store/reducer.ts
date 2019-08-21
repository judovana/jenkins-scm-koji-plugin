import { combineReducers } from "redux";

import configs from "./configs/reducer";

const rootReducer = combineReducers({
    configs: configs
});

export default rootReducer;

export type AppState = ReturnType<typeof rootReducer>;

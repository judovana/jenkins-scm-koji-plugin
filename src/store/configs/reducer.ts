import { ConfigsState, TaskType } from "../types";

const reducer = (state: ConfigsState = initialState, action: any): ConfigsState => {
    switch (action.type) {
        default:
            return state;
    }
}

const initialState: ConfigsState = {
    products: {
    },
    platforms: {
    },
    tasks: {
    },
    taskVariantCategories: {
    }
}

export default reducer;

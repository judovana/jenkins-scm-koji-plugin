import React from "react";
import { inject, observer } from "mobx-react";

import ConfigForm from "./ConfigForm";
import { CONFIG_STORE, ConfigStore } from "../stores/ConfigStore";

interface Props {
    configStore?: ConfigStore;
}

class Body extends React.PureComponent<Props> {

    componentDidMount() {
        const configStore = this.props.configStore!;
        configStore.fetchPlatforms();
        configStore.fetchProducts();
        configStore.fetchTasks();
        configStore.fetchTaskVariants();
        configStore.fetchJDKProjects();
    }

    render() {
        const configStore = this.props.configStore!;
        const { selectedGroup, selectedConfig, selectedGroupId } = configStore;
        return (
            <div className="Body">
                {
                    <div className="List">
                        <div>
                            {

                                selectedGroup.map(config =>
                                    <div
                                        className="ListItem"
                                        onClick={() => configStore.selectConfig(config)}
                                        key={config.id}>
                                        {config.id}
                                    </div>
                                )
                            }
                        </div>
                    </div>
                }
                {
                    selectedGroupId && selectedConfig && <ConfigForm
                        group={selectedGroupId}
                        config={selectedConfig}>
                    </ConfigForm>
                }
            </div>
        )
    }
}

export default inject(CONFIG_STORE)(observer(Body));

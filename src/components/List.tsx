import React from "react"
import { inject, observer } from "mobx-react"
import { CONFIG_STORE, ConfigStore } from "../stores/ConfigStore";

interface Props {
    configStore?: ConfigStore
}

class List extends React.PureComponent<Props> {

    render() {
        const { selectedGroup, selectConfig, selectedGroupId } = this.props.configStore!
        return (
            <div className="list-container">
                <div className="header">{selectedGroupId}</div>
                <div className="body">
                    {
                        selectedGroup.map((config, index) =>
                            <div
                                className="list-item"
                                onClick={() => selectConfig(config)}
                                key={config.id}
                                style={{marginBottom: index < selectedGroup.length ? 2 : 0}}>
                                {config.id}
                            </div>
                        )
                    }
                </div>
            </div>
        )
    }
}

export default inject(CONFIG_STORE)(observer(List))

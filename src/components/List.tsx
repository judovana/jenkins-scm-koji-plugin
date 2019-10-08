import React from "react"
import { inject, observer } from "mobx-react"
import { CONFIG_STORE, ConfigStore } from "../stores/ConfigStore";
import Button from "./Button";
import { Item } from "../stores/model";

interface Props {
    configStore?: ConfigStore
}

class List extends React.PureComponent<Props> {

    onCreate = () => {
        const configStore = this.props.configStore!
        configStore.selectNewConfig(configStore.selectedGroupId!)
    }

    onDelete = (config: Item) => {
        const configStore = this.props.configStore!
        configStore.deleteConfig(config.id)
    }

    render() {
        const { selectedGroup, selectConfig, selectedGroupId } = this.props.configStore!
        return (
            <div className="list-container">
                <div className="header">
                    <span>{selectedGroupId}</span>
                    <Button onClick={this.onCreate}>new</Button>
                </div>
                <div className="body">
                    {
                        selectedGroup.map((config, index) =>
                            <div
                                className="list-item"
                                onClick={() => selectConfig(config)}
                                key={config.id}
                                style={{marginBottom: index < selectedGroup.length ? 2 : 0}}>
                                {config.id}
                                <Button
                                    color="red"
                                    onClick={() => this.onDelete(config)}>delete</Button>
                            </div>
                        )
                    }
                </div>
            </div>
        )
    }
}

export default inject(CONFIG_STORE)(observer(List))

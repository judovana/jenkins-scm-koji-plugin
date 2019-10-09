import React from "react";
import { observer, inject } from "mobx-react";

import { ConfigStore, CONFIG_STORE } from "../stores/ConfigStore";

interface Props {
    configStore?: ConfigStore;
}

class Header extends React.PureComponent<Props> {

    componentDidMount() {
        const configStore = this.props.configStore!;
        configStore.fetchConfigs()
    }

    render() {
        const configStore = this.props.configStore!;
        return (
            <div className="header-container">
                {
                    configStore.configGroups.map(group =>
                        <div
                            className="header-item"
                            key={group.id}
                            onClick={() => configStore.selectGroup(group.id)}>
                            <div>{group.id}</div>
                        </div>
                    )
                }
            </div>
        );
    }
}

export default inject(CONFIG_STORE)(observer(Header));

import React from "react";
import { observer, inject } from "mobx-react";

import { ConfigStore, CONFIG_STORE } from "../stores/ConfigStore";

interface Props {
    configStore?: ConfigStore;
}

class Header extends React.PureComponent<Props> {

    render() {
        const configStore = this.props.configStore!;
        return (
            <div style={{
                margin: "0 auto",
                display: "flex",
                flexDirection: "row",
                justifyContent: "space-between",
                width: "80%"
            }}>
                {
                    configStore.configGroups.map(group =>
                        <div className="HeaderItem" key={group.id} onClick={() => configStore.selectGroup(group.id) }>
                            <div style={{ textAlign: "center", fontSize: 20 }}>{group.id}</div>
                        </div>
                    )
                }
            </div>
        );
    }
}

export default inject(CONFIG_STORE)(observer(Header));

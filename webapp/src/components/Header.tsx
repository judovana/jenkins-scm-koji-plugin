import React from "react";
import { observer, inject } from "mobx-react";

import { ConfigStore, CONFIG_STORE } from "../stores/ConfigStore";
import { useHistory, Link } from "react-router-dom"
import { AppBar, Tabs, Tab, Toolbar } from "@material-ui/core"

interface Props {
    configStore?: ConfigStore;
}

const Header: React.FC<Props> = props => {

    const configStore = props.configStore!
    const history = useHistory()

    React.useEffect(() => {
        configStore.fetchConfigs()
    })

    const onTabClick = (_: React.ChangeEvent<{}>, id: string) => {
        history.push(`/${id}`)
        configStore.selectGroup(id)
    }

    return (
        <React.Fragment>
            <AppBar position="fixed">
                <Tabs
                    onChange={onTabClick}
                    value={configStore.selectedGroupId}>
                    {
                        configStore.configGroups.map(({ id }) =>
                            <Tab
                                component={Link}
                                key={id}
                                label={id}
                                to={id}
                                value={id} />

                        )
                    }
                </Tabs>
            </AppBar>
            <Toolbar />
        </React.Fragment>
    )
}

export default inject(CONFIG_STORE)(observer(Header));

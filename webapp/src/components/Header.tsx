import React from "react"
import { useObserver } from "mobx-react"

import { AppBar, Tabs, Tab, Toolbar } from "@material-ui/core"
import useStores from "../hooks/useStores"

const Header: React.FC = () => {
    const { configStore, viewStore } = useStores()

    return useObserver(() => {
        const { selectedConfigGroupId } = configStore
        const { goToConfigList } = viewStore
        return (
            <React.Fragment>
                <AppBar position="fixed">
                    <Tabs value={selectedConfigGroupId}>
                        {configStore.configGroups.map(({ id }, index) => (
                            <Tab
                                onClick={_ => goToConfigList(id)}
                                key={index}
                                label={id}
                                value={id}
                            />
                        ))}
                    </Tabs>
                </AppBar>
                <Toolbar />
            </React.Fragment>
        )
    })
}

export default Header

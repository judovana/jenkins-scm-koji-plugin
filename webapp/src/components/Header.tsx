import React from "react";
import { useObserver } from "mobx-react"

import { useHistory, Link } from "react-router-dom"
import { AppBar, Tabs, Tab, Toolbar } from "@material-ui/core"
import useStores from "../hooks/useStores"

const Header: React.FC = () => {

    const { configStore } = useStores()
    const history = useHistory()

    return useObserver(() => {

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
                        {configStore.configGroups.map(({ id }) =>
                            <Tab
                                component={Link}
                                key={id}
                                label={id}
                                to={id}
                                value={id} />
                        )}
                    </Tabs>
                </AppBar>
                <Toolbar />
            </React.Fragment>
        )
    })
}

export default Header

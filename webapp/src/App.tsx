import React from "react"
import Header from "./components/Header"
import List from "./components/List"
import ConfigForm from "./components/configForms/ConfigForm"

import { Switch, Route } from "react-router-dom"
import { Grid } from "@material-ui/core"
import useStores from "./hooks/useStores"

const App: React.FC = () => {

    const { configStore } = useStores()

    React.useEffect(() => {
        configStore.fetchConfigs()
    }, [configStore])

    return (
        <React.Fragment>
            <Header />
            <Grid
                alignItems="center"
                container
                justify="center">
                <Grid item xs={11}>
                    <Switch>
                        <Route exact path="/:group">
                            <List />
                        </Route>
                        <Route path="/form/:group/:id?">
                            <ConfigForm />
                        </Route>
                    </Switch>
                </Grid>
            </Grid>
        </React.Fragment>
    );
}

export default App;

import React from "react"
import Header from "./components/Header"
import List from "./components/List"
import ConfigForm from "./components/configForms/ConfigForm"

import { Switch, Route } from "react-router-dom"

const App: React.FC = () => {
    return (
        <React.Fragment>
            <Header />
            <Switch>
                <Route exact path="/:group">
                    <List />
                </Route>
                <Route path="/form/:group/:id">
                    <ConfigForm />
                </Route>
            </Switch>
        </React.Fragment>
    );
}

export default App;

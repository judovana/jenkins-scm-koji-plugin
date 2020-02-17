import { createBrowserHistory } from "history"

import { ConfigStore } from "./ConfigStore"
import ConfigService from "./services/ConfigService"
import { ViewStore } from "./ViewStore"

const history = createBrowserHistory()
const url = window.location.origin

const service = new ConfigService(url)

const configStore = new ConfigStore(service)
const viewStore = new ViewStore(history, configStore)

export const stores = {
    configStore,
    viewStore
}

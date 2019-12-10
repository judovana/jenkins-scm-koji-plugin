import { ConfigStore } from "./ConfigStore"
import ConfigService from "./services/ConfigService"

const url = window.location.origin

const service = new ConfigService(url)

const configStore = new ConfigStore(service)

export const stores = {
    configStore
}

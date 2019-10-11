import { Item, ConfigGroup, ConfigGroups } from "../model";

class ConfigService {

    constructor(private readonly url: string) {
    }

    postConfig = async (groupId: string, config: Item): Promise<boolean> => {
        try {
            await fetch(`${this.url}/${groupId}`, {
                body: JSON.stringify(config),
                method: "POST"
            })
            return true
        } catch (error) {
            console.log(error)
            return false
        }

    }

    putConfig = async (groupId: string, config: Item): Promise<boolean> => {
        try {
            await fetch(`${this.url}/${groupId}/${config.id}`, {
                body: JSON.stringify(config),
                method: "PUT"
            })
            return true
        } catch (error) {
            console.log(error)
            return false
        }
    }

    deleteConfig = async (groupId: string, id: string): Promise<boolean> => {
        try {
            await fetch(`${this.url}/${groupId}/${id}`, {
                method: "DELETE"
            })
            return true
        } catch (error) {
            console.log(error)
            return false
        }
    }

    fetchConfigs = async (groupIds: string[]): Promise<ConfigGroups> => {
        const groups: ConfigGroups = {}
        for (const id of groupIds) {
            const group = await this.fetchConfig(id)
            groups[id] = group
        }
        return groups
    }

    fetchConfig = async (id: string): Promise<ConfigGroup> => {
        const response = await fetch(`${this.url}/${id}`)
        const configs: Item[] = await response.json()
        const configMap: ConfigGroup = {}
        configs.forEach(config =>
            configMap[config.id] = config
        )
        return configMap
    }
}

export default ConfigService

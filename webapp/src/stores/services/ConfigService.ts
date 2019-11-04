import { Item, ConfigGroup, ConfigGroups } from "../model";

class ConfigService {

    constructor(private readonly url: string) {
    }

    fetch = async (input: RequestInfo, init?: RequestInit): Promise<Response> => {
        try {
            const response = await fetch(`${this.url}/${input}`, init)
            if (response.status === 200) {
                return response
            }
            const errorMessage = await response.text()
            if (response.status === 400) {
                throw new Error("Bad Request:\n" + errorMessage)
            }
            if (response.status === 500) {
                throw new Error("Internal Error:\n" + errorMessage)
            }
            throw new Error("Unexpected response status: " + response.status)
        } catch (error) {
            throw error
        }
    }

    postConfig = async (groupId: string, config: Item): Promise<boolean> => {
        try {
            await this.fetch(`${groupId}`, {
                body: JSON.stringify(config),
                method: "POST"
            })
            return true
        } catch (error) {
            throw error
        }

    }

    putConfig = async (groupId: string, config: Item): Promise<boolean> => {
        try {
            await this.fetch(`${groupId}/${config.id}`, {
                body: JSON.stringify(config),
                method: "PUT"
            })
            return true
        } catch (error) {
            throw error
        }
    }

    deleteConfig = async (groupId: string, id: string): Promise<boolean> => {
        try {
            await this.fetch(`${groupId}/${id}`, {
                method: "DELETE"
            })
            return true
        } catch (error) {
            throw error
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
        const response = await this.fetch(`${id}`)
        const configs: Item[] = await response.json()
        const configMap: ConfigGroup = {}
        configs.forEach(config =>
            configMap[config.id] = config
        )
        return configMap

    }
}

export default ConfigService

import { observable, runInAction, action } from "mobx"

import {
    Platform,
    JDKVersion,
    TaskVariant,
    Task,
    JDKProject,
    Item,
    ConfigState,
    BuildProvider,
    JobUpdateResults,
    ConfigGroup,
    JDKTestProject,
    JDKTestProjectMap,
    ConfigMap,
    ConfigGroupId
} from "./model"
import ConfigService from "./services/ConfigService"
import defaults from "../utils/defaultConfigs"

export class ConfigStore {
    @observable
    private _configGroups: ConfigGroup[]

    @observable
    private _selectedGroupId: ConfigGroupId = "buildProviders"

    @observable
    private _selectedConfigId: string = ""

    @observable
    private _configState: ConfigState = "new"

    @observable
    private _configError?: string

    @observable
    private _jobUpdateResults?: JobUpdateResults

    @observable
    private _editedConfig: Item | null = null

    constructor(private readonly service: ConfigService) {
        this._configGroups = [
            {
                id: "buildProviders",
                configs: {}
            },
            {
                id: "jdkProjects",
                configs: {}
            },
            {
                id: "jdkTestProjects",
                configs: {}
            },
            {
                id: "jdkVersions",
                configs: {}
            },
            {
                id: "platforms",
                configs: {}
            },
            {
                id: "taskVariants",
                configs: {}
            },
            {
                id: "tasks",
                configs: {}
            }
        ]
    }

    @action
    discardOToolResponse = () => {
        this._jobUpdateResults = undefined
        this._configError = undefined
    }

    @action
    setJobUpdateResults = (results: JobUpdateResults) => {
        this._jobUpdateResults = results
        let result = ""
        result += "\nJobs created:\n"
        results.jobsCreated.forEach(res => {
            result +=
                res.jobName +
                (res.success ? ": success" : ": " + res.message) +
                "\n"
        })
        result += "\nJobs rewritten:\n"
        results.jobsRewritten.forEach(res => {
            result +=
                res.jobName +
                (res.success ? ": success" : ": " + res.message) +
                "\n"
        })
        result += "\nJobs revived:\n"
        results.jobsRevived.forEach(res => {
            result +=
                res.jobName +
                (res.success ? ": success" : ": " + res.message) +
                "\n"
        })
        result += "\nJobs archived:\n"
        results.jobsArchived.forEach(res => {
            result +=
                res.jobName +
                (res.success ? ": success" : ": " + res.message) +
                "\n"
        })
        console.log(result)
    }

    @action
    setError = (error?: string) => {
        this._configError = error
    }

    @action
    public setSelectedConfigGroupId = (id: ConfigGroupId) => {
        this._selectedGroupId = id
    }

    @action
    public setNewConfig = (configGroupId: ConfigGroupId) => {
        if (defaults[configGroupId]) {
            this._selectedGroupId = configGroupId
            this._editedConfig = defaults[configGroupId]()
            this._configState = "new"
        } else {
            this._editedConfig = null
        }
    }

    @action
    public setEditedConfig = (configGroupId: ConfigGroupId, configId: string) => {
        const group = this.configGroupMap[configGroupId]
        if (group) {
            const config = group.configs[configId]
            if (config) {
                this._selectedGroupId = configGroupId
                this._editedConfig = {...config}
                this._configState = "edit"
                return
            }
        }
        this._editedConfig = null
    }

    get jobUpdateResults(): JobUpdateResults | undefined {
        return this._jobUpdateResults
    }

    get configError(): string | undefined {
        return this._configError
    }

    get configState(): ConfigState {
        return this._configState
    }

    get configGroupMap(): { [id in ConfigGroupId]: ConfigGroup } {
        return this._configGroups.reduce((map, group) => {
            map[group.id] = group
            return map
        }, {} as { [id in ConfigGroupId]: ConfigGroup })
    }

    get configGroups(): ConfigGroup[] {
        return this._configGroups
    }

    public submit = () => {
        if (!this._editedConfig) {
            return
        }
        const groupId = this._selectedGroupId
        switch (this._configState) {
            case "edit":
                this.updateConfig(groupId, this._editedConfig)
                break
            case "new":
                this.createConfig(groupId, this._editedConfig)
                break
        }
    }

    createConfig = async (groupId: ConfigGroupId, config: Item) => {
        const response = await this.service.postConfig(groupId, config)
        if (response.value) {
            const oToolResponse = response.value
            const config = oToolResponse.config
            if (config) {
                runInAction(() => {
                    this.configGroupMap[groupId].configs[config.id] = {
                        ...oToolResponse.config!
                    }
                    this._selectedConfigId = config.id
                    this._configState = "edit"
                })
            }
            if (oToolResponse.jobUpdateResults) {
                this.setJobUpdateResults(oToolResponse.jobUpdateResults)
            }
        } else {
            this.setError(response.error!)
        }
    }

    updateConfig = async (groupId: ConfigGroupId, config: Item) => {
        const response = await this.service.putConfig(groupId, config)
        if (response.value) {
            const oToolResponse = response.value
            runInAction(() => {
                this.configGroupMap[groupId].configs[config.id] = {
                    ...oToolResponse.config!
                }
            })
            if (oToolResponse.jobUpdateResults) {
                this.setJobUpdateResults(oToolResponse.jobUpdateResults)
            }
        } else {
            this.setError(response.error!)
        }
    }

    deleteConfig = async (groupId: ConfigGroupId, id: string) => {
        const response = await this.service.deleteConfig(groupId, id)
        if (response.value) {
            const oToolResponse = response.value
            runInAction(() => {
                delete this.configGroupMap[groupId].configs[
                    oToolResponse.config!.id
                ]
                this._selectedConfigId = ""
            })
            if (oToolResponse.jobUpdateResults) {
                this.setJobUpdateResults(oToolResponse.jobUpdateResults)
            }
        } else {
            this.setError(response.error!)
        }
    }

    fetchConfigs = async () => {
        for (const group of this._configGroups) {
            const result = await this.service.fetchConfig(group.id)
            if (result.value) {
                const configMap: ConfigMap = {}
                result.value.forEach(config => (configMap[config.id] = config))
                runInAction(() => {
                    this.configGroupMap[group.id].configs = configMap
                })
            }
        }
    }

    get buildProviders(): BuildProvider[] {
        return Object.values(this.configGroupMap["buildProviders"].configs)
    }

    get platforms(): Platform[] {
        return Object.values(
            this.configGroupMap["platforms"].configs
        ) as Platform[]
    }

    getPlatform(id: string): Platform | undefined {
        return this.configGroupMap["platforms"].configs[id] as
            | Platform
            | undefined
    }

    get jdkVersions(): JDKVersion[] {
        return Object.values(
            this.configGroupMap["jdkVersions"].configs
        ) as JDKVersion[]
    }

    public getJDKVersion = (id: string): JDKVersion | undefined => {
        return this.configGroupMap["jdkVersions"].configs[id] as
            | JDKVersion
            | undefined
    }

    get taskVariants(): TaskVariant[] {
        return Object.values(
            this.configGroupMap["taskVariants"].configs
        ) as TaskVariant[]
    }

    get taskVariantsMap(): { [id: string]: TaskVariant } {
        return this.configGroupMap["taskVariants"].configs as {
            [id: string]: TaskVariant
        }
    }

    get tasks(): Task[] {
        return Object.values(this.configGroupMap["tasks"].configs) as Task[]
    }

    getTask(id: string): Task | undefined {
        return this.configGroupMap["tasks"].configs[id] as Task | undefined
    }

    get jdkProjects(): JDKProject[] {
        return Object.values(
            this.configGroupMap["jdkProjects"].configs
        ) as JDKProject[]
    }

    get jdkProjectsMap(): { [id: string]: JDKProject | undefined } {
        return this.configGroupMap["jdkProjects"].configs as {
            [id: string]: JDKProject | undefined
        }
    }

    get jdkTestProjects(): JDKTestProject[] {
        return Object.values(
            this.configGroupMap["jdkTestProjects"].configs
        ) as JDKTestProject[]
    }

    get jdkTestProjectMap(): JDKTestProjectMap {
        return this.configGroupMap["jdkTestProjects"]
            .configs as JDKTestProjectMap
    }

    get editedConfig(): Item | null {
        return this._editedConfig
    }

    get selectedConfigGroupId(): ConfigGroupId {
        return this._selectedGroupId
    }

    get selectedConfigId(): string {
        return this._selectedConfigId
    }
}

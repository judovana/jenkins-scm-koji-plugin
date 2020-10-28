import { action, autorun, observable, runInAction } from "mobx"

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
    ConfigGroupId,
    OToolResponse,
    FetchResult
} from "./model"
import ConfigService from "./services/ConfigService"
import defaults from "../utils/defaultConfigs"
import {
    ConfigValidator,
    validators,
    ConfigValidation,
    isConfigValid
} from "../utils/validators"
import { getJobNameGenerator } from "../utils/createJobName"

export class ConfigStore {
    @observable
    private _configGroups: ConfigGroup[]

    @observable
    private _selectedGroupId: ConfigGroupId = "platforms"

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

    @observable
    private _configValidator: ConfigValidator | null = null

    @observable
    private _configValidation: ConfigValidation | null = null

    @observable
    private _resultDialogOpen: boolean = false

    @observable
    private _jenkinsUrl: string | undefined

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
    public closeResultDialog = () => {
        this._resultDialogOpen = false
    }

    @action
    private showResultDialog = (results: JobUpdateResults) => {
        this._jobUpdateResults = results
        this._resultDialogOpen = true

    }

    private displayJobResults = autorun(() => {
        const results = this.jobUpdateResults
        if (!results) {
            return
        }
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
    })

    @action
    setError = (error?: string) => {
        this._configError = error
    }

    @action
    private setConfigState = (configState: ConfigState) => {
        this._configState = configState
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
            this._configValidator = validators[configGroupId]
            this._configState = "new"
        } else {
            this._editedConfig = null
        }
    }

    @action
    public validate = () => {
        if (this._editedConfig && this._configValidator) {
            this._configValidation = this._configValidator(
                this._editedConfig as any
            ) as ConfigValidation
        }
    }

    @action
    public setEditedConfig = (
        configGroupId: ConfigGroupId,
        configId: string
    ) => {
        const group = this.configGroupMap[configGroupId]
        if (group) {
            const config = group.configs[configId]
            if (config) {
                this._selectedGroupId = configGroupId
                this._editedConfig = JSON.parse(JSON.stringify(config))
                this._configValidator = validators[configGroupId]
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
        this.validate()
        if (!isConfigValid(this._configValidation as any)) {
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

    @action
    private handleResponse = (
        groupId: ConfigGroupId,
        configState: ConfigState
    ) => (result: FetchResult<OToolResponse>) => {
        const { value, error } = result
        if (value) {
            const { config, jobUpdateResults } = value
            if (config) {
                this.configGroupMap[groupId].configs[config.id] = {
                    ...config
                }
                this._selectedConfigId = config.id
                this.setEditedConfig(groupId, config.id)
                this._configState = "edit"
            }
            if (jobUpdateResults) {
                this.showResultDialog(jobUpdateResults)
            }
        }
        if (error) {
            this._configError = error
            this._configState = configState
        }
    }

    @action
    private setJenkinsUrl = (url: string) => {
        this._jenkinsUrl = url
    }

    @action
    private handleError = (configState: ConfigState) => (error: string) => {
        this._configError = error
        this._configState = configState
    }

    createConfig = (groupId: ConfigGroupId, config: Item) => {
        const configState = this._configState
        this.setConfigState("pending")
        this.service
            .postConfig(groupId, config)
            .then(this.handleResponse(groupId, configState))
            .catch(this.handleError(configState))
    }

    updateConfig = async (groupId: ConfigGroupId, config: Item) => {
        const configState = this._configState
        this.setConfigState("pending")
        this.service
            .putConfig(groupId, config)
            .then(this.handleResponse(groupId, configState))
            .catch(this.handleError(configState))
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
                this.showResultDialog(oToolResponse.jobUpdateResults)
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

    fetchJenkinsUrl = () => {
        this.service.fetchText("get/jenkinsUrl")
            .then(result => {
                if (result.error) {
                    // TODO
                }
                if (result.value) {
                    this.setJenkinsUrl(result.value)
                }
            })
    }

    get jobNameGenerator() {
        return getJobNameGenerator(this.taskVariantsMap, this._jenkinsUrl)
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

    get configValidation(): ConfigValidation | null {
        return this._configValidation
    }

    get selectedConfigGroupId(): ConfigGroupId {
        return this._selectedGroupId
    }

    get selectedConfigId(): string {
        return this._selectedConfigId
    }

    get resultDialogOpen(): boolean {
        return this._resultDialogOpen
    }

    get jenkinsUrl(): string | undefined {
        return this._jenkinsUrl
    }
}

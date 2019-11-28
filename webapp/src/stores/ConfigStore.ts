import { observable, runInAction, action } from "mobx";

import { Platform, Product, TaskVariant, Task, JDKProject, Item, ConfigState, BuildProvider, ConfigGroups, JobUpdateResults, ConfigGroup } from "./model";
import ConfigService from "./services/ConfigService";

export const CONFIG_STORE = "configStore";

export class ConfigStore {

    @observable
    private _configGroups: ConfigGroups

    @observable
    private _selectedGroupId: string | undefined = "tasks";

    @observable
    private _selectedConfig: Item | undefined;

    @observable
    private _configState: ConfigState = "create"

    @observable
    private _configError?: string

    @observable
    private _jobUpdateResults?: JobUpdateResults

    constructor(private readonly service: ConfigService) {
        this._configGroups = this.configGroups.reduce((map, group) => {
            map[group.id] = {}
            return map
        }, {} as ConfigGroups)
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
            result += res.jobName + ((res.success) ? ": success" : ": " + res.message) + "\n"
        })
        result += "\nJobs rewritten:\n"
        results.jobsRewritten.forEach(res => {
            result += res.jobName + ((res.success) ? ": success" : ": " + res.message) + "\n"
        })
        result += "\nJobs revived:\n"
        results.jobsRevived.forEach(res => {
            result += res.jobName + ((res.success) ? ": success" : ": " + res.message) + "\n"
        })
        result += "\nJobs archived:\n"
        results.jobsArchived.forEach(res => {
            result += res.jobName + ((res.success) ? ": success" : ": " + res.message) + "\n"
        })
        console.log(result)
    }

    @action
    setError = (error?: string) => {
        this._configError = error
    }

    @action
    selectGroup = (id: string) => {
        this._selectedGroupId = id;
        this._selectedConfig = undefined;
        this._configState = "update"
    }

    @action
    selectConfig = (config: Item) => {
        this._selectedConfig = config;
        this._configState = "update"
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

    get selectedGroupId(): string | undefined {
        return this._selectedGroupId;
    }

    get selectedGroup(): Item[] {
        const groupId = this._selectedGroupId || ""
        return Object.values(this._configGroups[groupId] || {})
    }

    get configGroups(): Item[] {
        return [{ id: "buildProviders" }, { id: "platforms" }, { id: "products" }, { id: "taskVariants" }, { id: "tasks" }, { id: "jdkProjects" }];
    }

    get selectedConfig(): Item | undefined {
        return this._selectedConfig;
    }

    createConfig = async (config: Item) => {
        const groupId = this._selectedGroupId
        if (!groupId) {
            this.setError("No group is selected!")
            return
        }
        const response = await this.service.postConfig(groupId, config)
        if (response.value) {
            const oToolResponse = response.value
            runInAction(() => {
                this._configGroups[groupId][config.id] = { ...oToolResponse.config! }
                this._selectedConfig = this._configGroups[groupId][config.id]
                this._configState = "update"
            })
            if (oToolResponse.jobUpdateResults) {
                this.setJobUpdateResults(oToolResponse.jobUpdateResults)
            }
        } else {
            this.setError(response.error!)
        }
    }

    updateConfig = async (config: Item) => {
        const groupId = this._selectedGroupId
        if (!groupId) {
            this.setError("No group is selected!")
            return
        }
        const response = await this.service.putConfig(groupId, config)
        if (response.value) {
            const oToolResponse = response.value
            runInAction(() => {
                this._configGroups[groupId][config.id] = { ...oToolResponse.config! }
            })
            if (oToolResponse.jobUpdateResults) {
                this.setJobUpdateResults(oToolResponse.jobUpdateResults)
            }
        } else {
            this.setError(response.error!)
        }
    }

    deleteConfig = async (id: string) => {
        const groupId = this._selectedGroupId
        if (!groupId) {
            this.setError("No group is selected!")
            return
        }
        const response = await this.service.deleteConfig(groupId, id)
        if (response.value) {
            const oToolResponse = response.value
            runInAction(() => {
                delete this._configGroups[groupId][oToolResponse.config!.id]
                const selectedConfig = this._selectedConfig
                if (selectedConfig && id === selectedConfig.id) {
                    this._selectedConfig = undefined
                }
            })
            if (oToolResponse.jobUpdateResults) {
                this.setJobUpdateResults(oToolResponse.jobUpdateResults)
            }
        } else {
            this.setError(response.error!)
        }
    }

    fetchConfigs = async () => {

        for (const group of this.configGroups) {
            const result = await this.service.fetchConfig(group.id)
            if (result.value) {
                const configMap: ConfigGroup = {}
                result.value.forEach(config =>
                    configMap[config.id] = config
                )
                runInAction(() => {
                    this._configGroups[group.id] = configMap
                })
            }
        }
    }

    get buildProviders(): BuildProvider[] {
        return Object.values(this._configGroups["buildProviders"])
    }

    get platforms(): Platform[] {
        return Object.values(this._configGroups["platforms"]) as Platform[]
    }

    getPlatform(id: string): Platform | undefined {
        return this._configGroups["platforms"][id] as Platform | undefined
    }

    get products(): Product[] {
        return Object.values(this._configGroups["products"]) as Product[]
    }

    getProduct(id: string): Product | undefined {
        return this._configGroups["products"][id] as Task | undefined
    }

    get taskVariants(): TaskVariant[] {
        return Object.values(this._configGroups["taskVariants"]) as TaskVariant[]
    }

    getTaskVariant(id: string): TaskVariant | undefined {
        return this._configGroups["taskVariants"][id] as TaskVariant | undefined
    }

    get tasks(): Task[] {
        return Object.values(this._configGroups["tasks"]) as Task[]
    }

    getTask(id: string): Task | undefined {
        return this._configGroups["tasks"][id] as Task | undefined
    }

    get jdkProjects(): JDKProject[] {
        return Object.values(this._configGroups["jdkProjects"]) as JDKProject[]
    }

    getJDKProject(id: string): JDKProject | undefined {
        return this._configGroups["jdkProjects"][id] as JDKProject | undefined
    }
}

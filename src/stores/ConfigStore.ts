import { observable, runInAction, action } from "mobx";

import { Platform, Product, TaskVariant, Task, JDKProject, Item, ConfigState, BuildProvider, ConfigGroups, ConfigGroup } from "./model";
import { defaultTask, defaultJDKProject } from "./defaults";

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

    constructor() {
        this._configGroups = {}
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

    @action
    selectNewConfig = (groupId: string) => {
        switch (groupId) {
            case "jdkProjects":
                this._selectedConfig = defaultJDKProject
                break
            case "tasks":
                this._selectedConfig = defaultTask
                break
            default:
                return
        }
        this._configState = "create"
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

    postConfig = async (config: Item) => {
        const groupId = this._selectedGroupId
        if (!groupId) {
            return
        }
        try {
            const response = await fetch(`http://localhost:8081/${groupId}`, {
                body: JSON.stringify(config),
                method: "POST"
            })
            if (response.status === 201) {

                this._configGroups[groupId][config.id] = {...config}
                this._selectedConfig = this._configGroups[groupId][config.id]
                this._configState = "update"
            }
        } catch (error) {
            console.log(error)
        }

    }

    putConfig = async (config: Item) => {
        const groupId = this._selectedGroupId
        if (!groupId) {
            return
        }
        try {
            const response = await fetch(`http://localhost:8081/${groupId}/${config.id}`, {
                body: JSON.stringify(config),
                method: "PUT"
            })
            if (response.status === 204) {
                this._configGroups[groupId][config.id] = { ...config }
            }
        } catch (error) {
            console.log(error)
        }
    }

    deleteConfig = async (id: string) => {
        const groupId = this._selectedGroupId
        if (!groupId) {
            return
        }
        try {
            const response = await fetch(`http://localhost:8081/${groupId}/${id}`, {
                method: "DELETE"
            })
            if (response.status === 200) {
                delete this._configGroups[groupId][id]
                const selectedConfig = this._selectedConfig
                if (selectedConfig && id === selectedConfig.id) {
                    this._selectedConfig = undefined
                }
            }
        } catch (error) {
            console.log(error)
        }
    }

    async fetchConfigs() {
        this.configGroups.forEach(configGroup => {
            this.fetchConfig(configGroup.id)
        })
    }

    async fetchConfig(id: string) {
        const response = await fetch(`http://localhost:8081/${id}`)
        const buildProviders: Item[] = await response.json()
        const buildProvidersMap: ConfigGroup = {}
        buildProviders.forEach(buildProvider =>
            buildProvidersMap[buildProvider.id] = buildProvider
        )
        runInAction(() => {
            this._configGroups[id] = buildProvidersMap
        })
    }

    get buildProviders(): BuildProvider[] {
        return Object.values(this._configGroups["buildProviders"])
    }

    get platforms(): Platform[] {
        return Object.values(this._configGroups["platforms"])
    }

    getPlatform(id: string): Platform | undefined {
        return this._configGroups["platforms"][id] as Task | undefined
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

const store = new ConfigStore();

export default store;

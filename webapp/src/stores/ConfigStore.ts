import { observable, runInAction, action } from "mobx";

import { Platform, Product, TaskVariant, Task, JDKProject, Item, ConfigState, BuildProvider, ConfigGroups } from "./model";
import { defaultTask, defaultJDKProject } from "./defaults";
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
    private _configError: string | null = null;

    constructor(private readonly service: ConfigService) {
        this._configGroups = {}
    }

    @action
    discardError = () => {
        this._configError = null
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

    get errorMessage(): string | null {
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

    onError = (error: Error) => {
        this._configError = error.message
    }

    createConfig = async (config: Item) => {
        const groupId = this._selectedGroupId
        if (!groupId) {
            return
        }
        try {
            await this.service.postConfig(groupId, config)
            runInAction(() => {
                this._configGroups[groupId][config.id] = { ...config }
                this._selectedConfig = this._configGroups[groupId][config.id]
                this._configState = "update"
            })
        } catch (error) {
            this.onError(error)
        }
    }

    updateConfig = async (config: Item) => {
        const groupId = this._selectedGroupId
        if (!groupId) {
            return
        }
        try {
            await this.service.putConfig(groupId, config)
            runInAction(() => {
                this._configGroups[groupId][config.id] = { ...config }
            })
        } catch (error) {
            this.onError(error)
        }
    }

    deleteConfig = async (id: string) => {
        const groupId = this._selectedGroupId
        if (!groupId) {
            return
        }
        try {
            await this.service.deleteConfig(groupId, id)
            runInAction(() => {
                delete this._configGroups[groupId][id]
                const selectedConfig = this._selectedConfig
                if (selectedConfig && id === selectedConfig.id) {
                    this._selectedConfig = undefined
                }
            })
        } catch (error) {
            this.onError(error)
        }
    }

    fetchConfigs = async () => {
        try {
            const configGroups = await this.service.fetchConfigs(this.configGroups.map(configGroup => configGroup.id))
            runInAction(() => {
                this._configGroups = configGroups
            })
        } catch (error) {
            this.onError(error)
        }

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

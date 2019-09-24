import { observable, runInAction, action } from "mobx";

import { Platform, Product, TaskVariant, Task, JDKProject, Item } from "./model";

export const CONFIG_STORE = "configStore";

export class ConfigStore {

    @observable
    private _platforms: Map<string, Platform>;

    @observable
    private _products: Map<string, Product>;

    @observable
    private _taskVariants: Map<string, TaskVariant>;

    @observable
    private _tasks: Map<string, Task>;

    @observable
    private _jdkProjects: Map<string, JDKProject>;

    @observable
    private _selectedGroupId: string | undefined = "tasks";

    @observable _selectedConfig: Item | undefined;

    constructor() {
        this._platforms = new Map<string, Platform>();
        this._products = new Map<string, Product>();
        this._taskVariants = new Map<string, TaskVariant>();
        this._tasks = new Map<string, Task>();
        this._jdkProjects = new Map<string, JDKProject>();
    }

    @action
    selectGroup(id: string) {
        this._selectedGroupId = id;
        this._selectedConfig = undefined;
    }

    @action
    selectConfig(config: Item) {
        this._selectedConfig = config;
    }

    get selectedGroupId(): string | undefined {
        return this._selectedGroupId;
    }

    get selectedGroup(): Item[] {
        switch (this._selectedGroupId) {
            case "platforms": return Array.from(this._platforms.values());
            case "products": return Array.from(this._products.values());
            case "taskVariants": return Array.from(this._taskVariants.values());
            case "tasks": return Array.from(this._tasks.values());
            case "jdkProjects": return Array.from(this._jdkProjects.values());
            default: return [];
        }
    }

    get configGroups(): Item[] {
        return [{ id: "platforms" }, { id: "products" }, { id: "taskVariants" }, { id: "tasks" }, { id: "jdkProjects" }];
    }

    get selectedConfig(): Item | undefined {
        return this._selectedConfig;
    }

    async fetchPlatforms() {
        const response = await fetch("http://localhost:8081/platforms");
        const platforms: Platform[] = await response.json();
        runInAction(() => platforms.forEach(platform => this._platforms.set(platform.id, platform)));
    }

    async fetchProducts() {
        const response = await fetch("http://localhost:8081/products");
        const products: Product[] = await response.json();
        runInAction(() => products.forEach(product => this._products.set(product.id, product)));
    }

    async fetchTasks() {
        const response = await fetch("http://localhost:8081/tasks");
        const tasks: Task[] = await response.json();
        runInAction(() => tasks.forEach(task => {
            this._tasks.set(task.id, task);
            this._selectedConfig = this._tasks.get("tck");
        }));
    }

    async fetchTaskVariants() {
        const response = await fetch("http://localhost:8081/taskVariants");
        const taskVariants: TaskVariant[] = await response.json();
        runInAction(() => taskVariants.forEach(taskVariant => this._taskVariants.set(taskVariant.id, taskVariant)));
    }

    async fetchJDKProjects() {
        const response = await fetch("http://localhost:8081/jdkProjects");
        const projects: JDKProject[] = await response.json();
        runInAction(() => {
            projects.forEach(project => this._jdkProjects.set(project.id, project));
        });
    }

    get platforms(): Platform[] {
        return Array.from(this._platforms.values());
    }

    getPlatform(id: string): Platform | undefined {
        return this._platforms.get(id);
    }

    get products(): Product[] {
        return Array.from(this._products.values());
    }

    getProduct(id: string): Product | undefined {
        return this._products.get(id);
    }

    get taskVariants(): TaskVariant[] {
        return Array.from(this._taskVariants.values());
    }

    getTaskVariant(id: string): TaskVariant | undefined {
        return this._taskVariants.get(id);
    }

    get tasks(): Task[] {
        return Array.from(this._tasks.values());
    }

    getTask(id: string): Task | undefined {
        return this._tasks.get(id);
    }

    get jdkProjects(): JDKProject[] {
        return Array.from(this._jdkProjects.values());
    }

    getJDKProject(id: string): JDKProject | undefined {
        return this._jdkProjects.get(id);
    }
}

const store = new ConfigStore();

export default store;

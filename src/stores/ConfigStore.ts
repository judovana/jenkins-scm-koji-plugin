import { observable, runInAction } from "mobx";

import { Platform, Product, TaskVariant, Task } from "./model";

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

    constructor() {
        this._platforms = new Map<string, Platform>();
        this._products = new Map<string, Product>();
        this._taskVariants = new Map<string, TaskVariant>();
        this._tasks = new Map<string, Task>();
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
        runInAction(() => tasks.forEach(task => this._tasks.set(task.id, task)));
    }

    async fetchTaskVariants() {
        const response = await fetch("http://localhost:8081/taskVariants");
        const taskVariants: TaskVariant[] = await response.json();
        runInAction(() => taskVariants.forEach(taskVariant => this._taskVariants.set(taskVariant.id, taskVariant)));
    }

    get platforms(): Map<string, Platform> {
        return this._platforms;
    }

    get products(): Map<string, Product> {
        return this._products;
    }

    get taskVariants(): Map<string, TaskVariant> {
        return this._taskVariants;
    }

    get tasks(): Map<string, Task> {
        return this._tasks;
    }
}

const store = new ConfigStore();

export default store;

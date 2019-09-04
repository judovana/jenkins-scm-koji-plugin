import { observable, runInAction } from "mobx";

import { Project, ProjectCategory, ProjectCategories } from "./model";

export const PROJECT_STORE = "projectStore";

export class ProjectStore {

    @observable
    projectCategories: ProjectCategories;

    @observable
    selectedProjectCategoryId: string;

    @observable
    private _projects: Map<string, Project>;

    @observable
    selectedProjectId: string;

    constructor() {
        this.projectCategories = {
            "jdkProjects": {
                id: "jdkProjects",
                label: "JDK Projects",
                description: ""
            }
        };
        this._projects = new Map();
        this.selectedProjectId = "";
        this.selectedProjectCategoryId = "";
    }

    async fetchJDKProjects() {
        const response  = await fetch(`http://localhost:8081/jdkProjects`);
        const projects: Project[] = await response.json();
        runInAction(() => {
            projects.forEach(project => this._projects.set(project.id, project));
        });
    }

    get selectedProjectCategory(): ProjectCategory {
        return this.projectCategories[this.selectedProjectCategoryId];
    }

    get projects(): Map<string, Project> {
        return this._projects;
    }

    get selectedProject(): Project | undefined {
        return this._projects.get(this.selectedProjectId);
    }
}

const projectStore = new ProjectStore();

export default projectStore;

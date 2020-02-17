import { action, observable, computed } from "mobx"
import { History } from "history"
import { ConfigGroupId } from "./model"
import { ConfigStore } from "./ConfigStore"

const CONFIGS = "/configs"
const EDIT_FORM = "/editform"
const NEW_FORM = "/newform"

type View =
    | "list"
    | "form"

export class ViewStore {

    @observable
    private _currentView: View = "list"

    constructor(
        private readonly history: History,
        private readonly configStore: ConfigStore
    ) {
        history.listen(location => this.handleURLChange(location.pathname))
        this.handleURLChange(history.location.pathname)
    }

    private handleURLChange = (pathname: string) => {
        if (pathname === "/") {
            return
        }
        if (pathname.startsWith(CONFIGS)) {
            const paths = pathname.split("/")
            if (paths.length === 3) {
                this.setCurrentView("list")
                this.configStore.setSelectedConfigGroupId(paths[2] as ConfigGroupId)
            }
            if (paths.length === 4 && paths[3] === NEW_FORM) {
                this.setCurrentView("form")
                this.configStore.setNewConfig(paths[2] as ConfigGroupId)
            }
            if (paths.length === 5 && paths[4] === EDIT_FORM) {
                this.setCurrentView("form")
                this.configStore.setEditedConfig(paths[3] as ConfigGroupId, paths[4])
            }
        } else {
            this.history.push("/")
        }
    }

    public goToConfigList = (groupId: ConfigGroupId) => {
        this.history.push(`${CONFIGS}/${groupId}`)
        this.setCurrentView("list")
        this.configStore.setSelectedConfigGroupId(groupId)
    }

    public goToConfigEditForm = (groupId: ConfigGroupId, configId: string) => {
        this.history.push(`${CONFIGS}/${groupId}/${configId}${EDIT_FORM}`)
        this.setCurrentView("form")
        this.configStore.setEditedConfig(groupId, configId)
    }

    public goToConfigNewForm = (groupId: ConfigGroupId) => {
        this.history.push(`${CONFIGS}/${groupId}${NEW_FORM}`)
        this.setCurrentView("form")
        this.configStore.setNewConfig(groupId)
    }

    @action
    private setCurrentView = (view: View) => {
        this._currentView = view
    }

    @computed
    get currentView(): View {
        return this._currentView
    }
}

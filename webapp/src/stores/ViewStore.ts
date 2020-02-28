import { action, observable, computed } from "mobx"
import { History } from "history"
import {
    match,
    MatchFunction,
    MatchResult,
    RegexpToFunctionOptions,
    TokensToRegexpOptions
} from "path-to-regexp"

import { ConfigGroupId } from "./model"
import { ConfigStore } from "./ConfigStore"
import { ParseOptions } from "querystring"

const CONFIGS = "configs"
const EDITFORM = "editform"
const NEWFORM = "newform"
const GROUPID = ":groupId"
const CONFIGID = ":configId"

const options: ParseOptions &
    TokensToRegexpOptions &
    RegexpToFunctionOptions = {
    decode: decodeURIComponent
}

const listRegexp = match(`/${CONFIGS}/${GROUPID}`, options)
const newFormRegexp = match(`/${CONFIGS}/${GROUPID}/${NEWFORM}`, options)
const editFormRegexp = match(
    `/${CONFIGS}/${GROUPID}/${CONFIGID}/${EDITFORM}`,
    options
)

interface Route {
    path: MatchFunction
    handler: (match: MatchResult<any>) => void
}

interface ListRouteParams {
    groupId: string
}

interface NewFormRouteParams {
    groupId: string
}

interface EditFormRouteParams {
    groupId: string
    configId: string
}

type View = "list" | "form"

export class ViewStore {
    private readonly routes: Route[] = [
        {
            path: listRegexp,
            handler: ({
                params: { groupId }
            }: MatchResult<ListRouteParams>) => {
                this.setConfigListView(groupId as ConfigGroupId)
            }
        },
        {
            path: newFormRegexp,
            handler: ({
                params: { groupId }
            }: MatchResult<NewFormRouteParams>) => {
                this.setConfigNewFormView(groupId as ConfigGroupId)
            }
        },
        {
            path: editFormRegexp,
            handler: ({
                params: { groupId, configId }
            }: MatchResult<EditFormRouteParams>) => {
                this.setConfigEditFormView(groupId as ConfigGroupId, configId)
            }
        },
        {
            path: match("/"),
            handler: () => {}
        },
        {
            path: match("/(.*)"),
            handler: () => {
                this.history.push("/")
            }
        }
    ]

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
        for (const route of this.routes) {
            const match = route.path(pathname)
            if (match === false) {
                continue
            }
            route.handler(match)
            break
        }
    }

    public goToConfigList = (groupId: ConfigGroupId) => {
        this.history.push(`/${CONFIGS}/${groupId}`)
        this.setConfigListView(groupId)
    }

    private setConfigListView = (groupId: ConfigGroupId) => {
        this.setCurrentView("list")
        this.configStore.setSelectedConfigGroupId(groupId)
    }

    public goToConfigEditForm = (groupId: ConfigGroupId, configId: string) => {
        this.history.push(`/${CONFIGS}/${groupId}/${configId}/${EDITFORM}`)
        this.setConfigEditFormView(groupId, configId)
    }

    private setConfigEditFormView = (
        groupId: ConfigGroupId,
        configId: string
    ) => {
        this.setCurrentView("form")
        this.configStore.setEditedConfig(groupId, configId)
    }

    public goToConfigNewForm = (groupId: ConfigGroupId) => {
        this.history.push(`/${CONFIGS}/${groupId}/${NEWFORM}`)
        this.setConfigNewFormView(groupId)
    }

    private setConfigNewFormView = (groupId: ConfigGroupId) => {
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

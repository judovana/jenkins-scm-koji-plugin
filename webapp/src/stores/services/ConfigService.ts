import { Item, OToolResponse, FetchResult } from "../model";

class ConfigService {

    constructor(private readonly url: string) {
    }

    fetchText = (input: RequestInfo, init?: RequestInit) => this._fetch<string>(false, input, init)

    fetch = <T>(input: RequestInfo, init?: RequestInit) => this._fetch<T>(true, input, init)

    private _fetch = async <T>(isJson: boolean, input: RequestInfo, init?: RequestInit): Promise<FetchResult<T>> => {

        const handleError = (
            error: any,
            resolve: (value: FetchResult<T> | PromiseLike<FetchResult<T>>) => void
        ) => {
            console.log("handling error: " + error)
            switch (error.constructor) {
                case Error:
                    resolve({ error: (error as Error).message })
                    break
                default:
                    resolve({ error: "Unknown error" })
            }
        }

        return new Promise<FetchResult<T>>(resolve => {
            fetch(`${this.url}/${input}`, init)
                .then(response => {
                    if (response.status === 200) {
                        const valuePromise = isJson
                            ? response.json()
                            : response.text()
                        valuePromise
                            .then(value => {
                                resolve({ value: value as T })
                            })
                            .catch(error => {
                                handleError(error, resolve)
                            })
                    }
                    if (response.status === 400 || response.status === 500) {
                        response.text().then(text => {
                            resolve({ error: text })
                        }).catch(error => {
                            handleError(error, resolve)
                        })
                    }
                })
                .catch(error => {
                    handleError(error, resolve)
                })
        })
    }

    postConfig = async (groupId: string, config: Item): Promise<FetchResult<OToolResponse>> => {
        return await this.fetch<OToolResponse>(`${groupId}`, {
            body: JSON.stringify(config),
            method: "POST"
        })

    }

    putConfig = async (groupId: string, config: Item): Promise<FetchResult<OToolResponse>> => {
        return await this.fetch(`${groupId}/${config.id}`, {
            body: JSON.stringify(config),
            method: "PUT"
        })
    }

    deleteConfig = async (groupId: string, id: string): Promise<FetchResult<OToolResponse>> => {
        return await this.fetch(`${groupId}/${id}`, {
            method: "DELETE"
        })
    }

    fetchConfig = async (id: string): Promise<FetchResult<Item[]>> => {
        return await this.fetch<Item[]>(`${id}`)
    }
}

export default ConfigService

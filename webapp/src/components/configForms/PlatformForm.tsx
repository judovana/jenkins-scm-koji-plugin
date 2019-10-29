import React from "react"

import { ConfigStore } from "../../stores/ConfigStore";
import { Platform, ConfigState } from "../../stores/model";
import { observable, runInAction } from "mobx";
import TextInput from "../formComponents/TextInput";

interface Props {
    platform: Platform
    configStore?: ConfigStore
}

class PlatformForm extends React.Component<Props> {

    @observable
    platform?: Platform

    @observable
    platformState?: ConfigState

    componentDidMount() {
        const platform = this.props.platform
        this.platformState = platform.id === "" ? "create" : "update"
        this.platform = { ...platform }
    }

    componentDidUpdate() {
        const platform = this.props.platform
        const state = this.props.configStore!.configState
        if (state !== this.platformState) {
            runInAction(() => {
                this.platform = { ...platform }
                this.platformState = state
            })
            return
        }
        if (state === "update" && this.platform!.id !== platform.id) {
            runInAction(() => {
                this.platform = { ...platform }
            })
        }
    }

    onOSChange = (value: string) => {
        this.platform!.os = value
    }

    onArchitectureChange = (value: string) => {
        this.platform!.architecture = value
    }

    onProviderChange = (value: string) => {
        this.platform!.provider = value
    }

    render() {
        const configStore = this.props.configStore!
        if (!this.platform) {
            return null
        }
        const configState = configStore.configState
        const { architecture, os, provider, tags, version, vmName, vmNodes } = this.platform;
        return (
            <fieldset>
                <TextInput
                    label={"os"}
                    onChange={this.onOSChange}
                    value={os} />
                <TextInput
                    label={"architecture"}
                    onChange={this.onArchitectureChange}
                    value={architecture} />
            </fieldset>
        )
    }
}

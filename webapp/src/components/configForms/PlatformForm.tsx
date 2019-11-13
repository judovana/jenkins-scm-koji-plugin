import React from "react"
import Select from "../formComponents/Select"
import { inject, observer } from "mobx-react"

import { ConfigStore, CONFIG_STORE } from "../../stores/ConfigStore"
import { Platform, ConfigState, Item } from "../../stores/model";
import { observable, runInAction } from "mobx";
import TextInput from "../formComponents/TextInput";
import Button from "../Button";

interface Props {
    platform: Platform
    configStore?: ConfigStore
    onSubmit: (item: Item, state: ConfigState) => void}

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

    onVersionChange = (value: string) => {
        this.platform!.version = value
    }

    onArchitectureChange = (value: string) => {
        this.platform!.architecture = value
    }

    onProviderChange = (value: string) => {
        this.platform!.provider = value
    }

    onVMNameChange = (value: string) => {
        this.platform!.vmName = value
    }

    onVMNodesChange = (value: string) => {
        this.platform!.vmNodes = value.split(" ")
    }

    onHWNodesChange = (value: string) => {
        this.platform!.hwNodes = value.split(" ")
    }

    onTagsChange = (value: string) => {
        this.platform!.tags = value.split(" ")
    }

    onSubmit = () => {
        const platform = this.platform!
        const filter = (value: string) => value.trim() !== ""
        platform.vmNodes = platform.vmNodes.filter(filter)
        platform.hwNodes = platform.hwNodes.filter(filter)
        platform.tags = platform.tags.filter(filter)
        this.props.onSubmit(this.platform!, this.platformState!)
    }

    render() {
        const configStore = this.props.configStore!
        if (!this.platform) {
            return null
        }
        const configState = configStore.configState
        const {
            architecture,
            os,
            provider,
            tags,
            version,
            vmName,
            vmNodes,
            hwNodes
        } = this.platform
        return (
            <fieldset>
                <TextInput
                    label={"os"}
                    onChange={this.onOSChange}
                    value={os} />
                <TextInput
                    label={"version"}
                    onChange={this.onVersionChange}
                    value={version} />
                <TextInput
                    label={"architecture"}
                    onChange={this.onArchitectureChange}
                    value={architecture} />
                <Select
                    label={"provider"}
                    onChange={this.onProviderChange}
                    options={["vagrant", "beaker"]}
                    value={provider} />
                <TextInput
                    label={"vm name"}
                    onChange={this.onVMNameChange}
                    value={vmName} />
                <TextInput
                    label={"vm nodes"}
                    onChange={this.onVMNodesChange}
                    value={vmNodes.join(" ")} />
                <TextInput
                    label={"hw nodes"}
                    onChange={this.onHWNodesChange}
                    value={hwNodes.join(" ")} />
                <TextInput
                    label={"tags"}
                    onChange={this.onTagsChange}
                    value={tags.join(" ")} />
                <Button onClick={this.onSubmit}>{configState}</Button>

                {JSON.stringify(this.platform)}
            </fieldset>
        )
    }
}

export default inject(CONFIG_STORE)(observer(PlatformForm))

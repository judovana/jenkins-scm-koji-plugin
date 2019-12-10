import React from "react"
import Select from "../formComponents/Select"
import { useLocalStore, useObserver } from "mobx-react"

import { Platform, Item } from "../../stores/model"
import TextInput from "../formComponents/TextInput"
import { Button } from "@material-ui/core"
import useStores from "../../hooks/useStores"

interface Props {
    platformID?: string
    onSubmit: (item: Item) => void
}

const PlatformForm: React.FC<Props> = props => {

    const { configStore } = useStores()

    const { platformID } = props

    const platform = useLocalStore<Platform>(() => ({
        architecture: "",
        hwNodes: [],
        id: "",
        os: "",
        provider: "",
        tags: [],
        version: "",
        vmName: "",
        vmNodes: []
    }))

    React.useEffect(() => {
        if (platformID === undefined || platformID === platform.id) {
            return
        }
        const _platform = configStore.getPlatform(platformID)
        if (!_platform) {
            return
        }
        platform.architecture = _platform.architecture || ""
        platform.hwNodes = _platform.hwNodes || []
        platform.id = _platform.id || ""
        platform.os = _platform.os || ""
        platform.provider = _platform.provider || ""
        platform.tags = _platform.tags || []
        platform.version = _platform.version || ""
        platform.vmName = _platform.vmName || ""
        platform.vmNodes = _platform.vmNodes || []
    })

    const onOSChange = (value: string) => {
        platform!.os = value
    }

    const onVersionChange = (value: string) => {
        platform!.version = value
    }

    const onArchitectureChange = (value: string) => {
        platform!.architecture = value
    }

    const onProviderChange = (value: string) => {
        platform!.provider = value
    }

    const onVMNameChange = (value: string) => {
        platform!.vmName = value
    }

    const onVMNodesChange = (value: string) => {
        platform!.vmNodes = value.split(" ")
    }

    const onHWNodesChange = (value: string) => {
        platform!.hwNodes = value.split(" ")
    }

    const onTagsChange = (value: string) => {
        platform!.tags = value.split(" ")
    }

    const onSubmit = () => {
        const filter = (value: string) => value.trim() !== ""
        platform.vmNodes = platform.vmNodes.filter(filter)
        platform.hwNodes = platform.hwNodes.filter(filter)
        platform.tags = platform.tags.filter(filter)
        props.onSubmit(platform)
    }

    return useObserver(() => {
        const {
            architecture,
            os,
            provider,
            tags,
            version,
            vmName,
            vmNodes,
            hwNodes
        } = platform

        return (
            <React.Fragment>
                <TextInput
                    label={"os"}
                    onChange={onOSChange}
                    value={os} />
                <TextInput
                    label={"version"}
                    onChange={onVersionChange}
                    value={version} />
                <TextInput
                    label={"architecture"}
                    onChange={onArchitectureChange}
                    value={architecture} />
                <Select
                    label={"provider"}
                    onChange={onProviderChange}
                    options={["vagrant", "beaker"]}
                    value={provider} />
                <TextInput
                    label={"vm name"}
                    onChange={onVMNameChange}
                    value={vmName} />
                <TextInput
                    label={"vm nodes"}
                    onChange={onVMNodesChange}
                    value={vmNodes.join(" ")} />
                <TextInput
                    label={"hw nodes"}
                    onChange={onHWNodesChange}
                    value={hwNodes.join(" ")} />
                <TextInput
                    label={"tags"}
                    onChange={onTagsChange}
                    value={tags.join(" ")} />
                <Button
                    onClick={onSubmit}
                    variant="contained">
                    {platformID === undefined ? "Create" : "Update"}
                </Button>
            </React.Fragment>
        )
    })
}

export default PlatformForm

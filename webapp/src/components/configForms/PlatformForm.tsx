import React from "react"
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
        id: "",
        os: "",
        providers: [],
        tags: [],
        version: "",
        vmName: "",
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
        platform.id = _platform.id || ""
        platform.os = _platform.os || ""
        platform.providers = _platform.providers || []
        platform.tags = _platform.tags || []
        platform.version = _platform.version || ""
        platform.vmName = _platform.vmName || ""
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

    const onVMNameChange = (value: string) => {
        platform!.vmName = value
    }

    const onTagsChange = (value: string) => {
        platform!.tags = value.split(" ")
    }

    const onSubmit = () => {
        const filter = (value: string) => value.trim() !== ""
        platform.tags = platform.tags.filter(filter)
        props.onSubmit(platform)
    }

    return useObserver(() => {
        const {
            architecture,
            os,
            tags,
            version,
            vmName,
        } = platform

        // TODO: Add platform provider form

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
                <TextInput
                    label={"vm name"}
                    onChange={onVMNameChange}
                    value={vmName} />
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

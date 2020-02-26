import React from "react"
import { useLocalStore, useObserver } from "mobx-react"

import { Platform, Item, TestStableYZupdates } from "../../stores/model"
import TextInput from "../formComponents/TextInput"
import { Button } from "@material-ui/core"
import useStores from "../../hooks/useStores"
import FormList from "../formComponents/FormList"
import VariableForm from "../formComponents/VariableForm"
import Select from "../formComponents/Select"

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
        versionNumber: "",
        vmName: "",
        testingYstream: "NaN",
        stableZstream: "NaN",
        variables: []
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
        platform.kojiArch = _platform.kojiArch || undefined
        platform.providers = _platform.providers || []
        platform.tags = _platform.tags || []
        platform.version = _platform.version || ""
        platform.versionNumber = _platform.versionNumber || ""
        platform.vmName = _platform.vmName || ""
        platform.testingYstream = _platform.testingYstream || "NaN"
        platform.stableZstream = _platform.stableZstream || "NaN"
        platform.variables = _platform.variables || []
    })

    const onOSChange = (value: string) => {
        platform!.os = value
    }

    const onVersionChange = (value: string) => {
        platform!.version = value
    }

    const onVersionNumberChange = (value: string) => {
        platform!.versionNumber = value
    }

    const onArchitectureChange = (value: string) => {
        platform!.architecture = value
    }

    const onKojiArchChange = (value: string) => {
        platform.kojiArch = value === "" ? undefined : value
    }

    const onVMNameChange = (value: string) => {
        platform!.vmName = value
    }

    const onTagsChange = (value: string) => {
        platform!.tags = value.split(" ")
    }

    const onStableZstreamChange = (value: string) => {
        platform!.stableZstream = value as TestStableYZupdates
    }

    const onTestingYstreamChange = (value: string) => {
        platform!.testingYstream = value as TestStableYZupdates
    }

    const onSubmit = () => {
        const filter = (value: string) => value.trim() !== ""
        platform.tags = platform.tags.filter(filter)
        props.onSubmit(platform)
    }

    return useObserver(() => {
        const {
            architecture,
            kojiArch,
            os,
            tags,
            version,
            versionNumber,
            vmName,
            testingYstream,
            stableZstream,
            variables
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
                    label={"versionNumber"}
                    onChange={onVersionNumberChange}
                    value={versionNumber} />
                <TextInput
                    label={"architecture"}
                    onChange={onArchitectureChange}
                    value={architecture} />
                <TextInput
                    label={"koji arch"}
                    onChange={onKojiArchChange}
                    value={kojiArch} />
                <TextInput
                    label={"vm name"}
                    onChange={onVMNameChange}
                    value={vmName} />
                <TextInput
                    label={"tags"}
                    onChange={onTagsChange}
                    value={tags.join(" ")} />
                <Select
                        label={"stable/zStream"}
                        onChange={onStableZstreamChange}
                        options={["NaN", "True", "False"]}
                        value={stableZstream} />
                <Select
                        label={"testing/yStream"}
                        onChange={onTestingYstreamChange}
                        options={["NaN", "True", "False"]}
                        value={testingYstream} />
                <FormList
                    data={variables}
                    label="custom variables"
                    renderItem={item => <VariableForm variable={item} />}
                />
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

import React from "react"
import { useObserver } from "mobx-react"

import { Platform } from "../../stores/model"
import TextInput from "../formComponents/TextInput"
import FormList from "../formComponents/FormList"
import VariableForm from "../formComponents/VariableForm"

interface Props {
    config: Platform
}

const PlatformForm: React.FC<Props> = props => {

    return useObserver(() => {
        const { config: platform } = props

        const onOSChange = (value: string) => {
            platform.os = value
        }

        const onVersionChange = (value: string) => {
            platform.version = value
        }

        const onVersionNumberChange = (value: string) => {
            platform.versionNumber = value
        }

        const onArchitectureChange = (value: string) => {
            platform.architecture = value
        }

        const onKojiArchChange = (value: string) => {
            platform.kojiArch = value === "" ? undefined : value
        }

        const onVMNameChange = (value: string) => {
            platform.vmName = value
        }

        const onTagsChange = (value: string) => {
            platform.tags = value.split(" ")
        }

        const {
            architecture,
            kojiArch,
            os,
            tags,
            version,
            versionNumber,
            vmName,
            variables
        } = platform

        // TODO: Add platform provider form

        return (
            <React.Fragment>
                <TextInput label={"os"} onChange={onOSChange} value={os} />
                <TextInput
                    label={"version"}
                    onChange={onVersionChange}
                    value={version}
                />
                <TextInput
                    label={"versionNumber"}
                    onChange={onVersionNumberChange}
                    value={versionNumber}
                />
                <TextInput
                    label={"architecture"}
                    onChange={onArchitectureChange}
                    value={architecture}
                />
                <TextInput
                    label={"koji arch"}
                    onChange={onKojiArchChange}
                    value={kojiArch}
                />
                <TextInput
                    label={"vm name"}
                    onChange={onVMNameChange}
                    value={vmName}
                />
                <TextInput
                    label={"tags"}
                    onChange={onTagsChange}
                    value={tags.join(" ")}
                />
                <FormList
                    data={variables}
                    label="custom variables"
                    renderItem={item => <VariableForm variable={item} />}
                />
            </React.Fragment>
        )
    })
}

export default PlatformForm

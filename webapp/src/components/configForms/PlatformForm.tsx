import React from "react"
import { useObserver } from "mobx-react"

import { Platform, TestStableYZupdates } from "../../stores/model"
import TextInput from "../formComponents/TextInput"
import FormList from "../formComponents/FormList"
import VariableForm from "../formComponents/VariableForm"
import {
    PlatformValidation,
    PlatformProviderValidation,
    setDefaultValidations,
    VariableValidation
} from "../../utils/validators"
import PlatformProviderForm from "../formComponents/PlatformProviderForm"
import {
    createDefaultVariable,
    createDefaultPlatfromProvider
} from "../../utils/defaultConfigs"
import Select from "../formComponents/Select"

interface Props {
    config: Platform
    validation?: PlatformValidation
}

const PlatformForm: React.FC<Props> = props => {
    return useObserver(() => {
        const { config: platform, validation } = props

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

        const onStableZstreamChange = (value: string) => {
            platform!.stableZstream = value as TestStableYZupdates
        }

        const onTestingYstreamChange = (value: string) => {
            platform!.testingYstream = value as TestStableYZupdates
        }

        const onTagsChange = (value: string) => {
            platform.tags = value.split(" ")
        }

        const {
            architecture,
            kojiArch,
            os,
            providers,
            tags,
            version,
            versionNumber,
            vmName,
            testingYstream,
            stableZstream,
            variables
        } = platform

        const {
            architecture: architectureValidation,
            kojiArch: kojiArchValidation,
            os: osValidation,
            tags: tagsValidation,
            version: versionValidation,
            versionNumber: versionNumberValidation,
            vmName: vmNameValidation
        } = validation || ({} as PlatformValidation)

        const providersValidation = setDefaultValidations<
            PlatformProviderValidation
        >(validation && validation.providers, providers)

        const variablesValidation = setDefaultValidations<VariableValidation>(
            validation && validation.variables,
            variables
        )

        return (
            <React.Fragment>
                <TextInput
                    label={"os"}
                    onChange={onOSChange}
                    validation={osValidation}
                    value={os}
                />
                <TextInput
                    label={"version"}
                    onChange={onVersionChange}
                    validation={versionValidation}
                    value={version}
                />
                <TextInput
                    label={"versionNumber"}
                    onChange={onVersionNumberChange}
                    validation={versionNumberValidation}
                    value={versionNumber}
                />
                <TextInput
                    label={"architecture"}
                    onChange={onArchitectureChange}
                    validation={architectureValidation}
                    value={architecture}
                />
                <TextInput
                    label={"koji arch"}
                    onChange={onKojiArchChange}
                    validation={kojiArchValidation}
                    value={kojiArch}
                />
                <TextInput
                    label={"vm name"}
                    onChange={onVMNameChange}
                    validation={vmNameValidation}
                    value={vmName}
                />
                <FormList
                    data={providers}
                    label="platform providers"
                    onAdd={createDefaultPlatfromProvider}
                    renderItem={(item, index) => (
                        <PlatformProviderForm
                            platformProvider={item}
                            validation={providersValidation[index]}
                        />
                    )}
                />
                <TextInput
                    label={"tags"}
                    onChange={onTagsChange}
                    validation={tagsValidation}
                    value={tags.join(" ")}
                />
                <Select
                    label={"stable/zStream"}
                    onChange={onStableZstreamChange}
                    options={["NaN", "True", "False"]}
                    value={stableZstream}
                />
                <Select
                    label={"testing/yStream"}
                    onChange={onTestingYstreamChange}
                    options={["NaN", "True", "False"]}
                    value={testingYstream}
                />
                <FormList
                    data={variables}
                    label="custom variables"
                    onAdd={createDefaultVariable}
                    renderItem={(item, index) => (
                        <VariableForm
                            validation={variablesValidation[index]}
                            variable={item}
                        />
                    )}
                />
            </React.Fragment>
        )
    })
}

export default PlatformForm

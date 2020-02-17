import React from "react"
import { useObserver } from "mobx-react-lite"

import { JDKTestProject } from "../../stores/model"
import TextInput from "../formComponents/TextInput"
import MultiSelect from "../formComponents/MultiSelect"
import JobConfigComponent from "../formComponents/JobConfigComponent"
import useStores from "../../hooks/useStores"
import ProductSelectForm from "../formComponents/JDKVersionSelectForm"

type JDKTestProjectFormProps = {
    config: JDKTestProject
}

const JDKTestProjectForm: React.FC<JDKTestProjectFormProps> = props => {
    const { configStore } = useStores()

    return useObserver(() => {
        const { buildProviders } = configStore
        const { config: project } = props

        const onIDChange = (value: string) => {
            project.id = value
        }

        const onBuildProvidersChange = (value: string[]) => {
            project.buildProviders = value
        }

        const onSubpackageBlacklistChange = (value: string) => {
            project.subpackageBlacklist = value.split(" ")
        }

        const onSubpackageWhitelistChange = (value: string) => {
            project.subpackageWhitelist = value.split(" ")
        }

        return (
            <React.Fragment>
                <TextInput
                    label={"name"}
                    value={project.id}
                    onChange={onIDChange}
                />
                <MultiSelect
                    label={"build providers"}
                    onChange={onBuildProvidersChange}
                    options={buildProviders.map(
                        buildProvider => buildProvider.id
                    )}
                    values={project.buildProviders}
                />
                <ProductSelectForm product={project.product} />
                <TextInput
                    label={"subpackage blacklist"}
                    value={project.subpackageBlacklist.join(" ")}
                    onChange={onSubpackageBlacklistChange}
                />
                <TextInput
                    label={"subpackage whitelist"}
                    value={project.subpackageWhitelist.join(" ")}
                    onChange={onSubpackageWhitelistChange}
                />
                <JobConfigComponent
                    jobConfig={project.jobConfiguration}
                    projectType={project.type}
                />
            </React.Fragment>
        )
    })
}

export default JDKTestProjectForm

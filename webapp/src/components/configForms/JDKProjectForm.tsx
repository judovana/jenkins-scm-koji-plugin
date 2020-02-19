import React from "react"
import { useObserver } from "mobx-react"

import { JDKProject } from "../../stores/model"
import JobConfigComponent from "../formComponents/JobConfigComponent"
import TextInput from "../formComponents/TextInput"
import { Chip, Box } from "@material-ui/core"
import MultiSelect from "../formComponents/MultiSelect"
import useStores from "../../hooks/useStores"
import ProductSelectForm from "../formComponents/JDKVersionSelectForm"
import { JDKProjectValidation } from "../../utils/validators"

interface Props {
    config: JDKProject
    validation?: JDKProjectValidation
}

const JDKProjectForm: React.FC<Props> = props => {
    const { configStore } = useStores()

    return useObserver(() => {
        const { config: jdkProject, validation } = props

        const onIdChange = (value: string) => {
            jdkProject.id = value
        }

        const onBuildProvidersChange = (values: string[]) => {
            jdkProject.buildProviders = values
        }

        const onUrlChange = (value: string) => {
            jdkProject.url = value
        }

        const { buildProviders } = configStore
        const { id, product, url } = validation || ({} as JDKProjectValidation)

        return (
            <React.Fragment>
                <TextInput
                    label={"id"}
                    validation={id}
                    value={jdkProject.id}
                    onChange={onIdChange}
                />
                <TextInput
                    label={"url"}
                    validation={url}
                    value={jdkProject.url}
                    onChange={onUrlChange}
                />
                {jdkProject.repoState && (
                    <Box>
                        <Chip label={jdkProject.repoState} />
                    </Box>
                )}
                <MultiSelect
                    label={"build providers"}
                    onChange={onBuildProvidersChange}
                    options={buildProviders.map(
                        buildProvider => buildProvider.id
                    )}
                    values={jdkProject.buildProviders}
                />
                <ProductSelectForm
                    product={jdkProject.product}
                    validation={product}
                />
                <JobConfigComponent
                    jobConfig={jdkProject.jobConfiguration}
                    projectType={jdkProject.type}
                />
            </React.Fragment>
        )
    })
}

export default JDKProjectForm

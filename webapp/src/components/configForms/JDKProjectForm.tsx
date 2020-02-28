import React from "react"
import { useObserver } from "mobx-react"

import { JDKProject } from "../../stores/model"
import JobConfigComponent from "../formComponents/JobConfigComponent"
import TextInput from "../formComponents/TextInput"
import { Chip, Box } from "@material-ui/core"
import MultiSelect from "../formComponents/MultiSelect"
import useStores from "../../hooks/useStores"
import ProductSelectForm from "../formComponents/JDKVersionSelectForm"
import FormList from "../formComponents/FormList"
import VariableForm from "../formComponents/VariableForm"
import {
    JDKProjectValidation,
    setDefaultValidations,
    VariableValidation
} from "../../utils/validators"
import { createDefaultVariable } from "../../utils/defaultConfigs"

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
        const { id, product, url } =
            validation || ({} as JDKProjectValidation)

        const variablesValidation = setDefaultValidations<VariableValidation>(
            validation && validation.variables,
            jdkProject.variables
        )

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
                <FormList
                    data={jdkProject.variables}
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

export default JDKProjectForm

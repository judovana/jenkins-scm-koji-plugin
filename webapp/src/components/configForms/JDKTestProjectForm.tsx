import React from "react"
import { useObserver } from "mobx-react-lite"

import { JDKTestProject } from "../../stores/model"
import TextInput from "../formComponents/TextInput"
import MultiSelect from "../formComponents/MultiSelect"
import JobConfigComponent from "../formComponents/JobConfigComponent"
import useStores from "../../hooks/useStores"
import ProductSelectForm from "../formComponents/JDKVersionSelectForm"
import FormList from "../formComponents/FormList"
import VariableForm from "../formComponents/VariableForm"
import {
    JDKTestProjectValidation,
    setDefaultValidations,
    VariableValidation
} from "../../utils/validators"
import { createDefaultVariable } from "../../utils/defaultConfigs"

type JDKTestProjectFormProps = {
    config: JDKTestProject
    validation?: JDKTestProjectValidation
}

const JDKTestProjectForm: React.FC<JDKTestProjectFormProps> = props => {
    const { configStore } = useStores()

    return useObserver(() => {
        const { buildProviders } = configStore
        const { config: project, validation } = props

        const onIDChange = (value: string) => {
            project.id = value
        }

        const onBuildProvidersChange = (value: string[]) => {
            project.buildProviders = value
        }

        const onSubpackageDenylistChange = (value: string) => {
            project.subpackageDenylist = value.split(" ")
        }

        const onSubpackageAllowlistChange = (value: string) => {
            project.subpackageAllowlist = value.split(" ")
        }

        const {
            id,
            product,
            subpackageDenylist,
            subpackageAllowlist,
        } = validation || ({} as JDKTestProjectValidation)

        const variablesValidation = setDefaultValidations<VariableValidation>(
            validation && validation.variables,
            project.variables
        )

        return (
            <React.Fragment>
                <TextInput
                    label={"name"}
                    validation={id}
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
                <ProductSelectForm
                    product={project.product}
                    validation={product}
                />
                <TextInput
                    label={"subpackage denylist"}
                    validation={subpackageDenylist}
                    value={project.subpackageDenylist.join(" ")}
                    onChange={onSubpackageDenylistChange}
                />
                <TextInput
                    label={"subpackage allowlist"}
                    validation={subpackageAllowlist}
                    value={project.subpackageAllowlist.join(" ")}
                    onChange={onSubpackageAllowlistChange}
                />
                <JobConfigComponent
                    jdkId={project.product.jdk}
                    jobConfig={project.jobConfiguration}
                    projectId={project.id}
                    projectType={project.type}
                />
                <FormList
                    data={project.variables}
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

export default JDKTestProjectForm

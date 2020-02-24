import React from "react"
import { useLocalStore, useObserver } from "mobx-react"

import { JDKProject, Item } from "../../stores/model"
import JobConfigComponent from "../formComponents/JobConfigComponent"
import TextInput from "../formComponents/TextInput"
import { Button, Chip, Box } from "@material-ui/core"
import MultiSelect from "../formComponents/MultiSelect"
import useStores from "../../hooks/useStores"
import ProductSelectForm from "../formComponents/JDKVersionSelectForm"

interface Props {
    jdkProjectID?: string
    onSubmit: (item: Item) => void
}

const JDKProjectForm: React.FC<Props> = props => {

    const { configStore } = useStores()

    const { jdkProjectID } = props

    const jdkProject = useLocalStore<JDKProject>(() => ({
        buildProviders: [],
        id: "",
        jobConfiguration: { platforms: [] },
        product: {
            jdk: "",
            packageName: ""
        },
        type: "JDK_PROJECT",
        url: ""
    }))

    React.useEffect(() => {
        if (jdkProjectID === undefined || jdkProjectID === jdkProject.id) {
            return
        }
        const _jdkProject = configStore.getJDKProject(jdkProjectID)
        if (!_jdkProject) {
            return
        }
        jdkProject.buildProviders = _jdkProject.buildProviders || []
        jdkProject.id = _jdkProject.id || ""
        jdkProject.jobConfiguration = _jdkProject.jobConfiguration || { platforms: {} }
        jdkProject.product = _jdkProject.product || ""
        jdkProject.repoState = _jdkProject.repoState
        jdkProject.type = _jdkProject.type || "JDK_PROJECT"
        jdkProject.url = _jdkProject.url || ""
    })

    const onIdChange = (value: string) => {
        jdkProject!.id = value
    }

    const onBuildProvidersChange = (values: string[]) => {
        jdkProject!.buildProviders = values
    }

    const onUrlChange = (value: string) => {
        jdkProject!.url = value
    }

    const onSubmit = () => {
        props.onSubmit(jdkProject)
    }

    return useObserver(() => {
        const { buildProviders } = configStore

        return (
            <React.Fragment>
                <TextInput
                    label={"id"}
                    value={jdkProject.id}
                    onChange={onIdChange} />
                <TextInput
                    label={"url"}
                    value={jdkProject.url}
                    onChange={onUrlChange} />
                {
                    jdkProject.repoState && <Box>
                        <Chip
                            label={jdkProject.repoState} />
                    </Box>
                }
                <MultiSelect
                    label={"build providers"}
                    onChange={onBuildProvidersChange}
                    options={buildProviders.map(buildProvider => buildProvider.id)}
                    values={jdkProject.buildProviders}
                />
                <ProductSelectForm product={jdkProject.product} />
                <JobConfigComponent
                    jobConfig={jdkProject.jobConfiguration}
                    projectType={jdkProject.type}/>
                <Button
                    onClick={onSubmit}
                    variant="contained">
                    {jdkProjectID === undefined ? "Create" : "Update"}
                </Button>
            </React.Fragment>
        )
    })
}

export default JDKProjectForm

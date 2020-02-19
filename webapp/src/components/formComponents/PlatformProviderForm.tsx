import React from "react"
import { useObserver } from "mobx-react"
import { Grid } from "@material-ui/core"

import { PlatformProvider } from "../../stores/model"
import TextInput from "./TextInput"
import { PlatformProviderValidation } from "../../utils/validators"

interface PlatformProviderFormProps {
    platformProvider: PlatformProvider
    validation?: PlatformProviderValidation
}

const PlatformProviderForm: React.FC<PlatformProviderFormProps> = props =>
    useObserver(() => {
        const { platformProvider, validation } = props

        const onIdChange = (value: string) => {
            platformProvider.id = value
        }

        const onHwNodesChange = (value: string) => {
            platformProvider.hwNodes = value.trim().split(" ")
        }

        const onVmNodesChange = (value: string) => {
            platformProvider.vmNodes = value.trim().split(" ")
        }

        const { hwNodes, id, vmNodes } =
            validation || ({} as PlatformProviderValidation)

        return (
            <React.Fragment>
                <Grid container item xs={9}>
                    <TextInput
                        label={"id"}
                        onChange={onIdChange}
                        validation={id}
                        value={platformProvider.id}
                    />
                    <TextInput
                        label={"HW nodes"}
                        onChange={onHwNodesChange}
                        validation={hwNodes}
                        value={platformProvider.hwNodes.join(" ")}
                    />
                    <TextInput
                        label={"VM nodes"}
                        onChange={onVmNodesChange}
                        validation={vmNodes}
                        value={platformProvider.vmNodes.join(" ")}
                    />
                </Grid>
            </React.Fragment>
        )
    })

export default PlatformProviderForm

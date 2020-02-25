import React from "react"
import { FormControl, FormLabel, FormGroup } from "@material-ui/core"
import { useObserver } from "mobx-react"

import Checkbox from "./Checkbox"
import Select from "./Select"

import { FileRequirements, BinaryRequirement } from "../../stores/model"

interface Props {
    fileRequirements: FileRequirements
}

const FileRequirementsForm: React.FunctionComponent<Props> = (props) => {

    return useObserver(() => {
        const { fileRequirements } = props

        const onBinaryChange = (value: string) => {
            fileRequirements.binary = value as BinaryRequirement
        }

        const onSourcesChange = (value: boolean) => {
            fileRequirements.source = value
        }

        const onNoarchChange = (value: boolean) => {
            fileRequirements.noarch = value
        }

        return (
            <FormControl margin="normal">
                <FormLabel>
                    file requirements
            </FormLabel>
                <FormGroup>
                    <Checkbox
                        label="require sources"
                        onChange={onSourcesChange}
                        value={fileRequirements.source} />
                    <Checkbox
                        label="require noarch"
                        onChange={onNoarchChange}
                        value={fileRequirements.noarch} />
                    <Select
                        label={"binary requirements"}
                        onChange={onBinaryChange}
                        options={["BINARY", "BINARIES"]}
                        value={fileRequirements.binary} />
                </FormGroup>
            </FormControl>
        )
    })
}

export default FileRequirementsForm

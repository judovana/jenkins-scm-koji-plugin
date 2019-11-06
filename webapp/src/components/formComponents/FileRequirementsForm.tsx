import React from "react"
import { FormControl, FormLabel, FormGroup } from "@material-ui/core"

import Checkbox from "./Checkbox"
import Select from "./Select"

import { FileRequirements } from "../../stores/model"

interface Props {
    fileRequirements: FileRequirements
    onBinaryChange: (value: string) => void
    onSourcesChange: (value: boolean) => void
}

const FileRequirementsForm: React.FunctionComponent<Props> = (props) => {

    const {
        fileRequirements,
        onBinaryChange,
        onSourcesChange
    } = props

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
                <Select
                    label={"binary requirements"}
                    onChange={onBinaryChange}
                    options={["BINARY", "BINARIES"]}
                    value={fileRequirements.binary} />
            </FormGroup>
        </FormControl>
    )
}

export default FileRequirementsForm

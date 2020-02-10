import React from "react"
import { useObserver } from "mobx-react"
import { FormControl, FormGroup } from "@material-ui/core"
import { Variable } from "../../stores/model"
import TextInput from "./TextInput"
import Checkbox from "./Checkbox"

interface VariableFormPros {
    variable: Variable
}

const VariableForm: React.FC<VariableFormPros> = ({ variable }) => {
    return useObserver(() => {
        const onNameChange = (value: string) => {
            variable.name = value
        }

        const onValueChange = (value: string) => {
            variable.value = value
        }

        const onCommentChange = (value: string) => {
            variable.comment = value ? value : undefined
        }

        const onCommentedOutChange = (value: boolean) => {
            variable.commentedOut = value
        }

        const onDefaultPrefixChagne = (value: boolean) => {
            variable.defaultPrefix = value
        }

        const { comment, commentedOut, defaultPrefix, name, value } = variable
        return (
            <FormControl>
                <FormGroup>
                    <TextInput
                        label="name"
                        onChange={onNameChange}
                        value={name}
                    />
                    <TextInput
                        label="value"
                        onChange={onValueChange}
                        value={value}
                    />
                    <TextInput
                        label="comment"
                        onChange={onCommentChange}
                        value={comment}
                    />
                    <Checkbox
                        label="commented out"
                        onChange={onCommentedOutChange}
                        value={commentedOut}
                    />
                    <Checkbox
                        label="default prefix"
                        onChange={onDefaultPrefixChagne}
                        value={defaultPrefix}
                    />
                </FormGroup>
            </FormControl>
        )
    })
}

export default VariableForm

import React from "react"
import { useObserver } from "mobx-react"
import { FormControl, FormGroup } from "@material-ui/core"
import { Variable } from "../../stores/model"
import TextInput from "./TextInput"
import Checkbox from "./Checkbox"
import { VariableValidation } from "../../utils/validators"

interface VariableFormPros {
    validation?: VariableValidation
    variable: Variable
}

const VariableForm: React.FC<VariableFormPros> = props =>
    useObserver(() => {
        const { validation, variable } = props
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

        const onExportedChagne = (value: boolean) => {
            variable.exported = value
        }

        const {
            comment,
            commentedOut,
            defaultPrefix,
            name,
            value,
            exported
        } = variable
        const {
            comment: commentValidation,
            name: nameValidation,
            value: valueValidation
        } = validation || {} as VariableValidation
        return (
            <FormControl>
                <FormGroup>
                    <TextInput
                        label="name"
                        onChange={onNameChange}
                        validation={nameValidation}
                        value={name}
                    />
                    <TextInput
                        label="value"
                        onChange={onValueChange}
                        validation={valueValidation}
                        value={value}
                    />
                    <TextInput
                        label="comment"
                        onChange={onCommentChange}
                        validation={commentValidation}
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
                    <Checkbox
                        label="exported"
                        onChange={onExportedChagne}
                        value={exported}
                    />
                </FormGroup>
            </FormControl>
        )
    })

export default VariableForm

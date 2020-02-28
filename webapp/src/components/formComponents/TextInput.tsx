import React from "react"
import { useObserver } from "mobx-react"
import { TextField } from "@material-ui/core"
import { BasicValidation } from "../../utils/validators"
import { TextFieldProps } from "@material-ui/core/TextField"

type TextInputPropsRequired = {
    label: string
}

type TextInputPropsOptional = {
    onChange: (value: string) => void
    validation?: BasicValidation
    value?: string
}

type TextInputProps = TextInputPropsRequired & TextInputPropsOptional

const TextInput: React.FC<TextInputProps> = props =>
    useObserver(() => {
        const { label, validation, value } = props

        const onChange = (event: React.ChangeEvent<HTMLInputElement>) => {
            props.onChange(event.target.value)
        }

        const errorProps: TextFieldProps =
            (validation &&
                validation !== "ok" && {
                    error: true,
                    helperText: validation
                }) ||
            {}
        return (
            <TextField
                {...errorProps}
                fullWidth
                label={label}
                margin={"normal"}
                value={value || ""}
                onChange={onChange}
            />
        )
    })

export default TextInput

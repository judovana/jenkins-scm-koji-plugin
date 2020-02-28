import React from "react"
import {
    FormControl,
    InputLabel,
    Select as MaterialSelect,
    MenuItem,
    FormHelperText
} from "@material-ui/core"
import { BasicValidation } from "../../utils/validators"

type SelectPropsRequired = {
    options: string[]
}

type SelectPropsOptional = {
    label?: string
    onChange: (value: string) => void
    validation?: BasicValidation
    value?: string
}

type SelectProps = SelectPropsRequired & SelectPropsOptional

const Select: React.FC<SelectProps> = props => {
    const { label, options, validation, value } = props

    const onChange = (
        event: React.ChangeEvent<{ name?: string | undefined; value: unknown }>
    ) => {
        props.onChange(event.target.value as string)
    }

    const isError = validation && validation !== "ok"

    return (
        <FormControl margin={"normal"} fullWidth error={isError}>
            {label && <InputLabel>{label}</InputLabel>}
            <MaterialSelect
                value={value || "NONE"}
                onChange={onChange}>
                <MenuItem value="NONE">None</MenuItem>
                {options.map(option => (
                    <MenuItem key={option} value={option}>
                        {option}
                    </MenuItem>
                ))}
            </MaterialSelect>
            {isError && <FormHelperText>{validation}</FormHelperText>}
        </FormControl>
    )
}

export default Select

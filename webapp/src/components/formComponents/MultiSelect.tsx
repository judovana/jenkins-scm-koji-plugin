import React from "react"
import { FormControl, FormLabel, FormGroup, FormControlLabel, Checkbox } from "@material-ui/core"
import { useObserver } from "mobx-react"

type MultiSelectPropsRequired = {
    options: string[]
}

type MultiSelectPropsOptional = {
    label?: string
    onChange: (values: string[]) => void
    values: string[]
}

type MultiSelectProps = MultiSelectPropsRequired & MultiSelectPropsOptional

const MultiSelect: React.FC<MultiSelectProps> = ({ label, onChange, options, values, }) => {

    const handleChange = (id: string) => {
        const index = values.indexOf(id)
        if (index < 0) {
            values.splice(0, 0, id)
        } else {
            values.splice(index, 1)
        }
        onChange(values)
    }

    return useObserver(() => {
        const multiSelect = (
            <FormGroup>
                {
                    options.map((option, index) =>
                        <FormControlLabel
                            key={index.toString()}
                            control={
                                <Checkbox
                                    checked={values.indexOf(option) >= 0}
                                    onChange={() => {
                                        handleChange(option)
                                    }}
                                    value={index} />
                            }
                            label={option} />
                    )
                }
            </FormGroup>
        )

        return (
            <FormControl margin="normal">
                <FormLabel>{label || ""}</FormLabel>
                {multiSelect}
            </FormControl>
        )
    })
}

export default MultiSelect

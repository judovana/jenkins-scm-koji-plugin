import React from "react"
import { FormControl, FormLabel, FormGroup, FormControlLabel, Checkbox } from "@material-ui/core";

type MultiSelectPropsRequired = {
    options: string[]
}

type MultiSelectPropsOptional = {
    label?: string
    onChange: (values: string[]) => void
    values: string[]
}

type MultiSelectProps = MultiSelectPropsRequired & MultiSelectPropsOptional

class MultiSelect extends React.PureComponent<MultiSelectProps> {

    static defaultProps: MultiSelectPropsOptional = {
        onChange: _ => { },
        values: []
    }

    onChange = (id: string, value: boolean, index: number) => {
        const values = this.props.values
        if (value) {
            values.splice(index, 0, id)
        } else {
            values.splice(index, 1)
        }
    }

    renderMultiSelect = () => {
        const { options, values } = this.props
        return (
            <FormGroup>
                {
                    options.map((option, index) =>
                        <FormControlLabel
                            key={index.toString()}
                            control={
                                <Checkbox
                                    checked={values.indexOf(option) >= 0}
                                    onChange={(_, checked) => {
                                        this.onChange(option, checked, index)
                                    }}
                                    value={index} />
                            }
                            label={option} />
                    )
                }
            </FormGroup>
        )
    }

    render() {
        const { label } = this.props
        if (!label) {
            return this.renderMultiSelect()
        }
        return (
            <FormControl margin="normal">
                <FormLabel>{label}</FormLabel>
                {this.renderMultiSelect()}
            </FormControl>
        )
    }
}

export default MultiSelect

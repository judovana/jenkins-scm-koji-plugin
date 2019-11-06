import React from "react"
import { FormControl, InputLabel, Select as MaterialSelect, MenuItem } from "@material-ui/core";

type SelectPropsRequired = {
    options: string[]
}

type SelectPropsOptional = {
    label?: string
    onChange: (value: string) => void
    value: string
}

type SelectProps = SelectPropsRequired & SelectPropsOptional

class Select extends React.PureComponent<SelectProps> {

    static defaultProps: SelectPropsOptional = {
        onChange: _ => { },
        value: ""
    }

    onChange = (event: React.ChangeEvent<{ name?: string | undefined; value: unknown; }>) => {
        this.props.onChange(event.target.value as string)
    }

    renderSelect = () => {
        const { value, options } = this.props
        return (
            <MaterialSelect
                defaultValue={""}
                value={value}
                onChange={this.onChange}>
                    <MenuItem value="">
                        None
                    </MenuItem>
                {
                    options.map(option =>
                        <MenuItem
                            key={option}
                            value={option}>
                            {option}
                        </MenuItem>
                    )
                }
            </MaterialSelect>
        )
    }

    render() {
        const label = this.props.label
        if (!label) {
            return (
                <div className="value-container">
                    {this.renderSelect()}
                </div>
            )
        }
        return (
            <FormControl margin={"normal"} fullWidth>
                <InputLabel>{label}</InputLabel>
                {this.renderSelect()}
            </FormControl>
        )
    }
}

export default Select

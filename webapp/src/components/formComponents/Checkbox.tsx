import React from "react"
import { FormControl, FormControlLabel, Checkbox as CheckboxMaterial } from "@material-ui/core";

type CheckboxPropsRequired = {
    label: string
}

type CheckboxPropsOptional = {
    onChange: (value: boolean) => void
    value: boolean
}

type CheckboxProps = CheckboxPropsRequired & CheckboxPropsOptional

class Checkbox extends React.PureComponent<CheckboxProps> {

    static defaultProps: CheckboxPropsOptional = {
        onChange: _ => null,
        value: false
    }

    onChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        this.props.onChange(event.target.checked)
    }

    render() {
        const { label, value } = this.props
        return (
            <FormControl margin="normal">
                <FormControlLabel
                    control={<CheckboxMaterial
                        checked={value}
                        onChange={this.onChange}
                    />}
                    label={label} />
            </FormControl>
        )
    }
}

export default Checkbox

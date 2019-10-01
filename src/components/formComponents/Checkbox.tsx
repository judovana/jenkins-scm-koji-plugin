import React from "react"

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
            <div className="field-container">
                <div className="label-container">
                    {label}
                </div>
                <div className="value-container">
                    <input
                        checked={value}
                        onChange={this.onChange}
                        type="checkbox" />
                </div>
            </div>
        )
    }
}

export default Checkbox

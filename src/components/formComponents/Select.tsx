import React from "react"

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

    onChange = (event: React.ChangeEvent<HTMLSelectElement>) => {
        this.props.onChange(event.target.value)
    }

    renderSelect = () => {
        const { value, options } = this.props
        return (
            <select
                defaultValue={value}
                onChange={this.onChange}>
                {
                    options.map(option =>
                        <option
                            key={option}
                            value={option}>
                            {option}
                        </option>
                    )
                }
            </select>
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
            <div className="field-container">
                <div className="label-container">
                    <label>{label}</label>
                </div>
                {this.renderSelect()}
            </div>
        )
    }
}

export default Select

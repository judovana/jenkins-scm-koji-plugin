import React from "react"

type SelectPropsRequired = {
    label: string
    options: string[]
}

type SelectPropsOptional = {
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

    render() {
        const { label, options, value } = this.props
        return (
            <div className="field-container">
                <div className="label-container">
                    <label>{label}</label>
                </div>
                <div className="value-container">
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
                </div>
            </div>
        )
    }
}

export default Select

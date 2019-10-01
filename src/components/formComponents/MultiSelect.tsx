import React from "react"
import Checkbox from "./Checkbox";

type MultiSelectPropsRequired = {
    label: string
    options: string[]
}

type MultiSelectPropsOptional = {
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

    render() {
        const { label, options, values } = this.props
        return (
            <div className="field-container">
                <div className="label-container">
                    <label>{label}</label>
                </div>
                <div className="value-container">
                    {
                        options.map((option, index) =>
                            <Checkbox
                                key={index}
                                label={option}
                                onChange={value => this.onChange(option, value, index)}
                                value={values.indexOf(option) >= 0} />
                        )
                    }
                </div>
            </div>
        )
    }
}

export default MultiSelect

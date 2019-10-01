import React from "react"
import { observer } from "mobx-react"

type TextInputPropsRequired = {
    label: string
}

type TextInputPropsOptional = {
    onChange: (value: string) => void
    placeholder: string
    value: string
}

type TextInputProps = TextInputPropsRequired & TextInputPropsOptional;

class TextInput extends React.PureComponent<TextInputProps> {

    static defaultProps: TextInputPropsOptional = {
        onChange: _ => null,
        placeholder: "",
        value: ""
    }

    onChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        this.props.onChange(event.target.value)
    }

    render() {
        const { label, placeholder, value } = this.props
        return (
            <div className="field-container">
                <div className="label-container">
                    <label>{label}</label>
                </div>
                <div className="value-container">
                    <input
                        placeholder={placeholder}
                        type="text"
                        value={value}
                        onChange={this.onChange} />
                </div>
            </div>
        )
    }
}

export default observer(TextInput);

import React from "react"

type TextAreaPropsOptional = {
    onChange: (value: string) => void
    placeholder: string
    value: string
}

type TextAreaPropsRequired = {
    label: string
}

type TextAreaProps = TextAreaPropsRequired & TextAreaPropsOptional

class TextArea extends React.PureComponent<TextAreaProps> {

    static defaultProps: TextAreaPropsOptional = {
        onChange: _ => {},
        placeholder: "",
        value: ""
    }

    onChange = (event: React.ChangeEvent<HTMLTextAreaElement>) => {
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
                    <textarea
                        placeholder={placeholder}
                        value={value}
                        onChange={this.onChange} />
                </div>
            </div>
        )
    }
}

export default TextArea;

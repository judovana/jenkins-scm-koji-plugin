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
        onChange: _ => null,
        placeholder: "",
        value: ""
    }

    onChange = (event: React.ChangeEvent<HTMLTextAreaElement>) => {
        this.props.onChange(event.target.value)
    }

    render() {
        const { label, placeholder, value } = this.props
        return (
            <div style={{ display: "flex", flexDirection: "column" }}>
                <label>{label}</label>
                <textarea
                    placeholder={placeholder}
                    value={value}
                    onChange={this.onChange} />
            </div>
        )
    }
}

export default TextArea;

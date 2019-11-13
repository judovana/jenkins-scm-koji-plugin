import React from "react"
import { FormControl, FormLabel, TextareaAutosize } from "@material-ui/core";

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
        onChange: _ => { },
        placeholder: "",
        value: ""
    }

    onChange = (event: React.ChangeEvent<HTMLTextAreaElement>) => {
        this.props.onChange(event.target.value)
    }

    render() {
        const { label, placeholder, value } = this.props
        return (
            <FormControl margin="normal" fullWidth>
                <FormLabel>
                    <label>{label}</label>
                </FormLabel>
                <TextareaAutosize
                    placeholder={placeholder}
                    value={value}
                    onChange={this.onChange} />
            </FormControl>
        )
    }
}

export default TextArea;

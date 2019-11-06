import React from "react"
import { observer } from "mobx-react"
import { TextField } from "@material-ui/core";

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
        const { label, value } = this.props
        return (
            <TextField
                fullWidth
                label={label}
                margin={"normal"}
                value={value}
                onChange={this.onChange} />
        )
    }
}

export default observer(TextInput);

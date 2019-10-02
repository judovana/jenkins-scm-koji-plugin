import React from "react"

import "../styles/Button.css"

type ButtonColor = "red" | "green" | "gray" | "blue"

type ButtonPropsOptional = {
    children: string | string[] | number | number[]
    onClick: () => void
    color: ButtonColor
}

type ButtonPropsRequired = {

}

type ButtonProps = ButtonPropsOptional & ButtonPropsRequired

class Button extends React.PureComponent<ButtonProps> {

    static defaultProps: ButtonPropsOptional = {
        children: "",
        onClick: () => { },
        color: "blue"
    }

    render() {
        const { children, onClick, color } = this.props
        return (
            <div
                className={`button button-${color}`}
                onClick={onClick}>
                {children}
            </div>
        )
    }
}

export default Button

import React from "react";
import { Item } from "../stores/model";

interface Props {
    values: Item[];
    value?: string;
    label: string;
    onChange: (value: string) => void;
}

interface State {
    expanded: boolean;
}

class Dropdown extends React.PureComponent<Props, State> {

    constructor(props: Props) {
        super(props);
        this.state = {
            expanded: false
        }
    }

    handleSelect = (value: string): void => {
        this.props.onChange(value);
        this.setState({ expanded: false });
    }

    handleClick = (): void => {
        this.setState({ expanded: !this.state.expanded });
    }

    getLabel = (): string => {
        const formItem: Item | undefined = this.props.values.find(value => this.props.value === value.id);
        if (!formItem) {
            return this.props.label;
        }
        return formItem.label;
    }

    render() {
        return (
            <div>
                <button onClick={this.handleClick}>{this.getLabel()}</button>
                {
                    !this.state.expanded ? null :
                        <div>
                            {
                                this.props.values.map((value, index) =>
                                    <div
                                        onClick={() => this.handleSelect(value.id)}
                                        key={index.toString()}>
                                        {value.label}
                                    </div>
                                )
                            }
                        </div>
                }
            </div>
        );
    }
}

export default Dropdown;

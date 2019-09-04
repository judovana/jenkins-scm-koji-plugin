import React from "react";
import { Item } from "../stores/model";

interface Props {
    items: Item[];
    label: string;
    onAdd: (id: string) => void;
}

interface State {
    expanded: boolean;
}

class AddComponent extends React.PureComponent<Props, State> {

    constructor(props: Props) {
        super(props);
        this.state = {
            expanded: false
        }
    }

    handleAdd = (id: string): void => {
        this.props.onAdd(id);
        this.setState({ expanded: false });
    }

    handleClick = (): void => {
        this.setState({ expanded: !this.state.expanded });
    }

    render() {
        const { label, items } = this.props;
        return (
            <div>
                <button onClick={this.handleClick}>{label}</button>
                {
                    !this.state.expanded ? null :
                        <div>
                            {
                                items.map((item, index) =>
                                    <div
                                        onClick={() => this.handleAdd(item.id)}
                                        key={index.toString()}>
                                        {item.label}
                                    </div>
                                )
                            }
                        </div>
                }
            </div>
        );
    }
}

export default AddComponent;

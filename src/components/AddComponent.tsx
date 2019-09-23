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
            <div className="Dropdown">
                <button
                    className="DropdownLabel"
                    onClick={this.handleClick}>{label}</button>
                {
                    !this.state.expanded ? null :
                        <div className="DropdownList">
                            {
                                items.map((item, index) =>
                                    <div
                                    className="DropdownListItem"
                                        onClick={() => this.handleAdd(item.id)}
                                        key={index.toString()}>
                                        {item.id}
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

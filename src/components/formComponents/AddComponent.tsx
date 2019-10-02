import React from "react";
import { Item } from "../../stores/model";

interface Props {
    items: Item[];
    label: string;
    onAdd: (id: string) => void;
}

class AddComponent extends React.PureComponent<Props> {

    onClick = (value: string): void => {
        this.props.onAdd(value);
    }

    render() {
        const { label, items } = this.props;
        return (
            <div
                className="dropdown"
                defaultValue="Add">
                <div className="dropdown-button">{label}</div>
                <div className="dropdown-content">
                    {
                        items.map(item =>
                            <a
                                onClick={() => this.onClick(item.id)}>
                                {item.id}</a>
                        )
                    }
                </div>
            </div>
        );
    }
}

export default AddComponent;

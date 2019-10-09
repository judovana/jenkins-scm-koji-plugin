import React from "react";
import { Item } from "../../stores/model";
import Button from "../Button"

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
                <Button>{label}</Button>
                <div className="dropdown-content">
                    {
                        items.map(item =>
                            <span
                                key={item.id}
                                onClick={() => this.onClick(item.id)}>
                                {item.id}</span>
                        )
                    }
                </div>
            </div>
        );
    }
}

export default AddComponent;

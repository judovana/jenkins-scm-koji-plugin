import React from "react";
import { Item } from "../../stores/model";
import AddComponent from "./AddComponent";
import { observer } from "mobx-react";

interface ListProps {
    label: string;
    addedValues: string[];
    items: Item[];
    onDelete: (index: number) => void;
    onAdd: (id: string) => void;
}

class List extends React.PureComponent<ListProps> {

    render() {
        const { label, addedValues, items, onAdd, onDelete } = this.props;
        const addedItems = items.filter(item => addedValues.indexOf(item.id) >= 0);
        const itemsToAdd = items.filter(item => addedValues.indexOf(item.id) < 0);
        return (
            <div>
                <div>{label}</div>
                <div>
                    {
                        addedItems.map((item, index) =>
                            <ListItem
                                index={index}
                                item={item}
                                key={index}
                                onDelete={onDelete} />
                        )
                    }
                </div>
                {
                    itemsToAdd.length > 0 && <AddComponent
                    items={itemsToAdd}
                    onAdd={onAdd}
                    label={"+"} />
                }
            </div>
        );
    }
}

interface ItemProps {
    index: number;
    item: Item;
    onDelete: (index: number) => void;
}

const ListItem: React.StatelessComponent<ItemProps> = (props) => {
    const { index, item, onDelete } = props;
    return (
        <div style={{ display: "flex" }}>
            <div>{item.id}</div>
            <div onClick={() => onDelete(index)}>X</div>
        </div>
    );
};

export default observer(List);

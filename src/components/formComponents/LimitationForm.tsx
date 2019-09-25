import React from "react";
import { observer } from "mobx-react";

import { Limitation, Item, LimitFlag } from "../../stores/model";
import Dropdown from "./Dropdown";
import List from "./List";

interface LimitationProps {
    label: string;
    limitation: Limitation;
    items: Item[];
}

class LimitationForm extends React.PureComponent<LimitationProps> {

    onDelete = (index: number) => {
        this.props.limitation.list.splice(index, 1);
    }

    onAdd = (id: string) => {
        this.props.limitation.list.push(id);
    }

    render() {
        const { label, limitation, items } = this.props;
        return (
            <div>
                <div style={{ display: "flex" }}>
                    <div>{label}</div>
                    <Dropdown
                        label={"select..."}
                        values={[{ id: "WHITELIST" }, { id: "BLACKLIST" }]}
                        value={limitation.flag}
                        onChange={(value) => { limitation.flag = value as LimitFlag }}
                    />
                </div>
                <List
                    addedValues={limitation.list}
                    items={items}
                    label={""}
                    onAdd={this.onAdd}
                    onDelete={this.onDelete} />
            </div>
        );
    }
}

export default observer(LimitationForm);

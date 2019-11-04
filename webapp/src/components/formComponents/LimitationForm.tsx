import React from "react";
import { observer } from "mobx-react";

import { Limitation, Item, LimitFlag } from "../../stores/model";
import Select from "./Select";
import MultiSelect from "./MultiSelect";

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

    onFlagChange = (value: string) => {
        this.props.limitation.flag = value as LimitFlag
    }

    onListChange = (values: string[]) => {
        this.props.limitation.list = values
    }

    render() {
        const { label, limitation, items } = this.props;
        const flag = limitation && (limitation.flag || "NONE")
        return (
            <div className="field-container">
                <div className="label-container">
                    {label}
                </div>
                <div className="value-container">
                    <Select
                        onChange={this.onFlagChange}
                        options={["NONE", "WHITELIST", "BLACKLIST"]}
                        value={flag}
                    />
                    {
                        (flag !== "NONE") &&
                        <MultiSelect
                            onChange={this.onListChange}
                            options={items.map(item => item.id)}
                            values={limitation.list} />
                    }
                </div>
            </div>
        );
    }
}

export default observer(LimitationForm);

import React from "react";
import { TaskConfig, VariantsConfig } from "../stores/model";
import VariantComponent from "./VariantComponent";
import { inject, observer } from "mobx-react";
import { CONFIG_STORE, ConfigStore } from "../stores/ConfigStore";

interface Props {
    id: string;
    config: TaskConfig;
    configStore?: ConfigStore;
    onDelete: (id: string) => void;
    onChange: (config: TaskConfig) => void;
}


class TaskComponent extends React.PureComponent<Props> {

    handleVariantChange = (index: number, config: VariantsConfig): void => {
        const { onChange, config: task } = this.props;
        onChange({
            ...task,
            variants: [
                ...task.variants.splice(0, index),
                config,
                ...task.variants.splice(index + 1)
            ]
        });
    }

    onAdd = (): void => {
        const variants = [...this.props.config.variants];
        variants.push({ map: {} });
        this.props.onChange({
            ...this.props.config,
            variants
        });
    }

    onVariantDeletion = (index: number): void => {
        const variants = [...this.props.config.variants];
        variants.splice(index, 1);
        this.props.onChange({
            ...this.props.config,
            variants
        })
    }

    render() {
        const { configStore, id, config, onDelete } = this.props;
        if (!configStore) {
            return null;
        }
        const task = configStore.tasks.get(id);
        return (
            <div style={container}>
                <div style={{ display: "flex", flexDirection: "row" }}>
                    {id}
                    <button onClick={this.onAdd}>Add variant</button>
                    <button onClick={() => onDelete(id)}>X</button>
                </div>
                {
                    task && config.variants.map((variant, index) =>
                        <div key={index}>
                            <VariantComponent
                                type={task.type}
                                onChange={variant => this.handleVariantChange(index, variant)}
                                onDelete={() => this.onVariantDeletion(index)}
                                config={variant} />
                        </div>
                    )
                }
            </div>
        );
    }
}

export default inject(CONFIG_STORE)(observer(TaskComponent));

const container: React.CSSProperties = {
    marginLeft: 10
}

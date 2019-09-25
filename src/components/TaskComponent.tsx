import React from "react";
import { TaskConfig } from "../stores/model";
import VariantComponent from "./VariantComponent";
import { inject, observer } from "mobx-react";
import { CONFIG_STORE, ConfigStore } from "../stores/ConfigStore";
import TreeNode from "./TreeNode";

interface Props {
    id: string;
    config: TaskConfig;
    configStore?: ConfigStore;
    onDelete: (id: string) => void;
    level: number;
}


class TaskComponent extends React.PureComponent<Props> {

    onAdd = (): void => {
        this.props.config.variants.push({ map: {} });
    }

    onVariantDelete = (index: number): void => {
        this.props.config.variants.splice(index, 1);
    }

    render() {
        const { configStore, id, config, onDelete } = this.props;
        const task = configStore!.getTask(id);
        if (!task) {
            return (
                <div>uknown task</div>
            )
        }
        const variantConfigs = config.variants
        return (
            <TreeNode level={this.props.level + 1}>
                <TreeNode.Title level={this.props.level + 1}>{id}</TreeNode.Title>
                <TreeNode.NodeInfo>Variants ({variantConfigs.length})</TreeNode.NodeInfo>
                <TreeNode.Options>
                    {[
                        <button key="add" onClick={this.onAdd} className="Add">+</button>,
                        <button
                            key="remove"
                            className="Remove"
                            onClick={() => onDelete(id)}>X</button>
                    ]}
                </TreeNode.Options>
                <TreeNode.ChildNodes>
                    {
                        variantConfigs.map((variantConfig, index) =>
                            <VariantComponent
                                key={index}
                                type={task.type}
                                onDelete={() => this.onVariantDelete(index)}
                                config={variantConfig}
                                level={this.props.level + 1} />
                        )
                    }
                </TreeNode.ChildNodes>
            </TreeNode>
        )
    }
}

export default inject(CONFIG_STORE)(observer(TaskComponent));

import React from "react";

import PlatformComponent from "./PlatformComponent";

import { VariantsConfig, TaskType } from "../stores/model";
import Dropdown from "./Dropdown";
import AddComponent from "./AddComponent";
import { inject, observer } from "mobx-react";
import { CONFIG_STORE, ConfigStore } from "../stores/ConfigStore";
import TreeNode from "./TreeNode";

interface Props {
    type: TaskType;
    configStore?: ConfigStore;
    config: VariantsConfig;
    onDelete: () => void;
    level: number;
}

class VariantComponent extends React.PureComponent<Props> {

    onVariantChange = (id: string, value: string): void => {
        this.props.config.map[id] = value;
    }

    onPlatformAdd = (id: string): void => {
        const config = this.props.config;
        if (!config.platforms) {
            config.platforms = {};
        }
        config.platforms[id] = { tasks: {} };
    }

    onPlatformDelete = (id: string): void => {
        delete this.props.config.platforms![id];
    }

    render() {
        const { config, configStore, type, onDelete } = this.props;
        const taskVariants = Array.from(configStore!.taskVariants.values()).filter(taskVariant => taskVariant.type === type);
        const platformConfigs = config.platforms || {};
        const selectedPlatformIds = Object.keys(platformConfigs);
        const unselectedPlatforms = Array.from(configStore!.platforms.values()).filter(platform => !selectedPlatformIds.includes(platform.id));
        return (
            <TreeNode level={this.props.level + 1}>
                <TreeNode.Title level={this.props.level + 1}>
                    <div style={{ display: "flex", flexDirection: "row" }}>
                        {
                            taskVariants.map(taskVariant =>
                                <Dropdown
                                    values={Object.values(taskVariant.variants)}
                                    label={taskVariant.id}
                                    value={this.props.config.map[taskVariant.id]}
                                    onChange={(value: string) => this.onVariantChange(taskVariant.id, value)}
                                    key={taskVariant.id} />
                            )
                        }
                    </div>
                </TreeNode.Title>
                <TreeNode.NodeInfo>
                    <div>Platforms ({Object.keys(platformConfigs).length})</div>
                </TreeNode.NodeInfo>
                <TreeNode.Options>{[
                    unselectedPlatforms.length !== 0 && type !== TaskType.TEST && <AddComponent
                        key="add"
                        onAdd={this.onPlatformAdd}
                        items={unselectedPlatforms}
                        label={"Add platform"} />,
                    <button
                        key="remove"
                        className="Remove"
                        onClick={() => onDelete()}>X</button>
                ]}</TreeNode.Options>
                <TreeNode.ChildNodes>
                    {
                        Object.keys(platformConfigs).map(id =>
                            <PlatformComponent
                                key={id}
                                id={id}
                                onDelete={this.onPlatformDelete}
                                config={platformConfigs[id]}
                                type={TaskType.TEST}
                                level={this.props.level + 1} />
                        )
                    }
                </TreeNode.ChildNodes>
            </TreeNode>
        );
    }
}

export default inject(CONFIG_STORE)(observer(VariantComponent));

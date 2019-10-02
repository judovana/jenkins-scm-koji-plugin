import React from "react";

import PlatformComponent from "./PlatformComponent";

import { VariantsConfig, TaskType } from "../stores/model";
import Select from "./formComponents/Select";
import AddComponent from "./formComponents/AddComponent";
import { inject, observer } from "mobx-react";
import { CONFIG_STORE, ConfigStore } from "../stores/ConfigStore";
import TreeNode from "./TreeNode";
import Button from "./Button";

interface Props {
    type: TaskType;
    configStore?: ConfigStore;
    config: VariantsConfig;
    onDelete: () => void;
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
        const taskVariants = configStore!.taskVariants.filter(taskVariant => taskVariant.type === type);
        const platformConfigs = config.platforms || {};
        const selectedPlatformIds = Object.keys(platformConfigs);
        const unselectedPlatforms = Array.from(configStore!.platforms.values()).filter(platform => !selectedPlatformIds.includes(platform.id));
        const isExtendable = type !== "TEST"
        return (
            <TreeNode>
                <TreeNode.Title>
                    <div style={{ display: "flex", flexDirection: "column" }}>
                        {
                            taskVariants.map(taskVariant =>
                                <Select
                                    options={Object.values(taskVariant.variants).map(variant => variant.id)}
                                    label={taskVariant.id}
                                    value={this.props.config.map[taskVariant.id]}
                                    onChange={(value: string) => this.onVariantChange(taskVariant.id, value)}
                                    key={taskVariant.id} />
                            )
                        }
                    </div>
                </TreeNode.Title>
                <TreeNode.NodeInfo>
                    {
                        isExtendable &&
                        <div>Platforms ({selectedPlatformIds.length})</div>
                    }
                </TreeNode.NodeInfo>
                <TreeNode.Options>{[
                    unselectedPlatforms.length !== 0 && isExtendable && <AddComponent
                        key="add"
                        onAdd={this.onPlatformAdd}
                        items={unselectedPlatforms}
                        label={"Add platform"} />,
                    <Button
                        key="remove"
                        color="red"
                        onClick={() => onDelete()}>Remove</Button>
                ]}</TreeNode.Options>
                <TreeNode.ChildNodes>
                    {
                        isExtendable &&
                        selectedPlatformIds.map(id =>
                            <PlatformComponent
                                key={id}
                                id={id}
                                onDelete={this.onPlatformDelete}
                                config={platformConfigs[id]}
                                type={"TEST"} />
                        )
                    }
                </TreeNode.ChildNodes>
            </TreeNode>
        );
    }
}

export default inject(CONFIG_STORE)(observer(VariantComponent));

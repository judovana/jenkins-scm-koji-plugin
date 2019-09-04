import React from "react";

import PlatformComponent from "./PlatformComponent";

import { VariantsConfig, PlatformConfig, TaskType, Item } from "../stores/model";
import Dropdown from "./Dropdown";
import AddComponent from "./AddComponent";
import { inject, observer } from "mobx-react";
import { CONFIG_STORE, ConfigStore } from "../stores/ConfigStore";

interface Props {
    type: TaskType;
    configStore?: ConfigStore;
    config: VariantsConfig;
    onChange: (config: VariantsConfig) => void;
    onDelete: () => void;
}

class VariantComponent extends React.PureComponent<Props> {

    handleVariantChange = (id: string, value: string): void => {
        const { onChange, config: variant } = this.props;
        onChange({
            ...variant,
            map: {
                ...variant.map,
                [id]: value
            }
        });
    }

    handlePlatformChange = (id: string, platformConfig: PlatformConfig = { tasks: {} }): void => {
        const { onChange, config: variant } = this.props;
        onChange({
            ...variant,
            platforms: {
                ...variant.platforms,
                [id]: platformConfig
            }
        })
    }

    handlePlatformDeletion = (id: string): void => {
        const config = { ...this.props.config };
        if (!config.platforms) {
            return;
        }
        delete config.platforms[id];
        this.props.onChange(config);
    }

    renderVariants = () => {
        const { configStore, type } = this.props;
        if (!configStore) {
            return null;
        }
        const platforms = Array.from(configStore.platforms.values());
        const platformConfigs = this.props.config.platforms;
        const selectedPlatformIds = platformConfigs ? Object.keys(platformConfigs) : [];
        const unselectedPlatforms = platforms.filter(platform => !selectedPlatformIds.includes(platform.id));
        const taskVariants = Array.from(configStore.taskVariants.values()).filter(taskVariant => taskVariant.type === type);
        return (
            <div style={variantContainer}>
                {
                    taskVariants.map(taskVariant =>
                        <Dropdown
                            values={Object.values(taskVariant.variants)}
                            label={taskVariant.label}
                            value={this.props.config.map[taskVariant.id]}
                            onChange={(value: string) => this.handleVariantChange(taskVariant.id, value)}
                            key={taskVariant.id} />
                    )
                }
                {
                    unselectedPlatforms.length === 0 || this.props.type === TaskType.TEST ? null :
                        <AddComponent
                            onAdd={this.handlePlatformChange}
                            items={unselectedPlatforms as Item[]}
                            label={"Add platform"} />
                }
                <button onClick={this.props.onDelete}>X</button>
            </div>
        )
    }

    renderPlatforms = () => {
        const platformConfigs = this.props.config.platforms;
        return (
            <div>
                {
                    !platformConfigs ? null :
                        Object.keys(platformConfigs).map(id =>
                            <div key={id}>
                                <PlatformComponent
                                    onChange={config => this.handlePlatformChange(id, config)}
                                    onDelete={this.handlePlatformDeletion}
                                    config={platformConfigs[id]}
                                    id={id}
                                    type={TaskType.TEST} />
                            </div>
                        )
                }
            </div>
        );
    }

    render() {
        return (
            <div style={container}>
                {this.renderVariants()}
                {this.renderPlatforms()}
            </div>
        );
    }
}

export default inject(CONFIG_STORE)(observer(VariantComponent));

const container: React.CSSProperties = {
    marginLeft: 20,
    marginTop: 5
}

const variantContainer: React.CSSProperties = {
    display: "flex",
    flexDirection: "row"
}

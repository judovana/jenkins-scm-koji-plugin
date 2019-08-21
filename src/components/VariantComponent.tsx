import React from "react";

import PlatformComponent from "./PlatformComponent";

import { VariantsConfig, TaskVariantCategory, Platform, PlatformConfig, TaskType } from "../store/types";
import Dropdown from "./Dropdown";
import { AppState } from "../store/reducer";
import { connect } from "react-redux";
import AddComponent from "./AddComponent";

interface Props {
    type: TaskType;
    categories: TaskVariantCategory[];
    variant: VariantsConfig;
    onChange: (config: VariantsConfig) => void;
    onDelete: () => void;
}

interface StateProps {
    platforms: { [id: string]: Platform };
}

class VariantComponent extends React.PureComponent<Props & StateProps> {

    handleVariantChange = (id: string, value: string): void => {
        const { onChange, variant } = this.props;
        onChange({
            ...variant,
            map: {
                ...variant.map,
                [id]: value
            }
        });
    }

    handlePlatformChange = (id: string, platformConfig: PlatformConfig = { tasks: {} }): void => {
        const { onChange, variant } = this.props;
        onChange({
            ...variant,
            platforms: {
                ...variant.platforms,
                [id]: platformConfig
            }
        })
    }

    handlePlatformDeletion = (id: string): void => {
        const config = { ...this.props.variant };
        if (!config.platforms) {
            return;
        }
        delete config.platforms[id];
        this.props.onChange(config);
    }

    renderVariants = () => {
        const platformConfigs = this.props.variant.platforms;
        const selectedPlatformIds = platformConfigs ? Object.keys(platformConfigs) : [];
        const unselectedPlatforms = Object.values(this.props.platforms).filter(platform => !selectedPlatformIds.includes(platform.id));
        return (
            <div style={variantContainer}>
                {
                    this.props.categories.map(category =>
                        <Dropdown
                            values={category.variants}
                            label={category.label}
                            value={this.props.variant.map[category.id]}
                            onChange={(value: string) => this.handleVariantChange(category.id, value)}
                            key={category.id} />
                    )
                }
                {
                    unselectedPlatforms.length === 0 || this.props.type === TaskType.TEST ? null :
                        <AddComponent
                            onAdd={this.handlePlatformChange}
                            items={unselectedPlatforms}
                            label={"Add platform"} />
                }
                <button onClick={this.props.onDelete}>X</button>
            </div>
        )
    }

    renderPlatforms = () => {
        const platforms = this.props.platforms;
        const platformConfigs = this.props.variant.platforms;
        return (
            <div>
                {
                    !platformConfigs ? null :
                        Object.keys(platformConfigs).map(id =>
                            <div key={id}>
                                <PlatformComponent
                                    onChange={(config) => this.handlePlatformChange(id, config)}
                                    onDelete={this.handlePlatformDeletion}
                                    config={platformConfigs[id]}
                                    platform={platforms[id]}
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

const mapStateToProps = (state: AppState): StateProps => ({
    platforms: state.configs.platforms
})

export default connect(mapStateToProps)(VariantComponent);

const container: React.CSSProperties = {
    marginLeft: 20,
    marginTop: 5
}

const variantContainer: React.CSSProperties = {
    display: "flex",
    flexDirection: "row"
}

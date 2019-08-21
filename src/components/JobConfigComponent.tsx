import React from "react";
import { JobConfig, Platform, PlatformConfig, TaskType } from "../store/types";
import PlatformComponent from "./PlatformComponent";
import { AppState } from "../store/reducer";
import { connect } from "react-redux";
import AddComponent from "./AddComponent";

interface Props {
    jobConfig: JobConfig;
    onChange: (config: JobConfig) => void;
}

interface StateProps {
    platforms: { [id: string]: Platform };
}

class JobConfigComponent extends React.PureComponent<Props & StateProps> {

    handlePlatformChange = (id: string, platformConfig: PlatformConfig = { tasks: {} }): void => {
        const { onChange, jobConfig } = this.props;
        onChange({
            ...jobConfig,
            platforms: {
                ...jobConfig.platforms,
                [id]: platformConfig
            }
        })
    }

    handlePlatformDeletion = (id: string): void => {
        const config = { ...this.props.jobConfig };
        delete config.platforms[id];
        this.props.onChange(config);
    }

    render() {
        const platformConfigs = this.props.jobConfig.platforms;
        const unselectedPlatforms = Object.values(this.props.platforms).filter(platform => !Object.keys(platformConfigs).includes(platform.id));
        return (
            <div>
                {
                    Object.keys(platformConfigs).map(id =>
                        <div key={id}>
                            <PlatformComponent
                                onChange={(config) => this.handlePlatformChange(id, config)}
                                onDelete={this.handlePlatformDeletion}
                                config={platformConfigs[id]}
                                platform={this.props.platforms[id]}
                                type={TaskType.BUILD} />
                        </div>
                    )
                }
                {
                    unselectedPlatforms.length === 0 ? null :
                        <AddComponent
                            onAdd={this.handlePlatformChange}
                            items={unselectedPlatforms}
                            label={"Add platform"} />
                }
            </div>
        );
    }
}

const mapStateToProps = (state: AppState): StateProps => {
    const platforms = state.configs.platforms;
    return {
        platforms: platforms
    }
}

export default connect(mapStateToProps)(JobConfigComponent);

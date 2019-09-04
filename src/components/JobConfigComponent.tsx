import React from "react";
import { JobConfig, PlatformConfig, TaskType } from "../stores/model";
import PlatformComponent from "./PlatformComponent";
import AddComponent from "./AddComponent";
import { ConfigStore, CONFIG_STORE } from "../stores/ConfigStore";
import { inject, observer } from "mobx-react";

interface Props {
    jobConfig: JobConfig;
    onChange: (config: JobConfig) => void;
    configStore?: ConfigStore;
}

class JobConfigComponent extends React.PureComponent<Props> {

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
        const configStore = this.props.configStore;
        if (!configStore) {
            return null;
        }
        const platforms = configStore.platforms;
        const unselectedPlatforms = Array.from(platforms.values()).filter(platform => !Object.keys(platformConfigs).includes(platform.id));
        return (
            <div>
                {
                    Object.keys(platformConfigs).map(id => {
                        return (
                            <div key={id}>
                                <PlatformComponent
                                    id={id}
                                    onChange={(config: PlatformConfig) => this.handlePlatformChange(id, config)}
                                    onDelete={this.handlePlatformDeletion}
                                    config={platformConfigs[id]}
                                    type={TaskType.BUILD} />
                            </div>
                        )
                    })
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

export default inject(CONFIG_STORE)(observer(JobConfigComponent));

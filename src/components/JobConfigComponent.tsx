import React from "react";
import { JobConfig, PlatformConfig, TaskType } from "../stores/model";
import PlatformComponent from "./PlatformComponent";
import AddComponent from "./AddComponent";
import { ConfigStore, CONFIG_STORE } from "../stores/ConfigStore";
import { inject, observer } from "mobx-react";
import TreeNode from "./TreeNode";

interface Props {
    jobConfig: JobConfig;
    configStore?: ConfigStore;
}

class JobConfigComponent extends React.PureComponent<Props> {

    onPlatformAdd = (id: string, platformConfig: PlatformConfig = { tasks: {} }): void => {
        this.props.jobConfig.platforms[id] = platformConfig;
    }

    onPlatformDelete = (id: string): void => {
        delete this.props.jobConfig.platforms[id];
    }

    render() {
        const platformConfigs = this.props.jobConfig.platforms;
        const configStore = this.props.configStore!;
        const platforms = configStore.platforms;
        const unselectedPlatforms = Array.from(platforms.values()).filter(platform => !Object.keys(platformConfigs).includes(platform.id));
        return (
            <div>
                <TreeNode level={0}>
                    <TreeNode.Title level={0}>
                        Job Configuration
                    </TreeNode.Title>
                    <TreeNode.NodeInfo>
                        Platforms ({Object.keys(platformConfigs).length})
                    </TreeNode.NodeInfo>
                    <TreeNode.Options>
                        {
                            unselectedPlatforms.length !== 0 && <AddComponent
                                onAdd={this.onPlatformAdd}
                                items={unselectedPlatforms}
                                label={"Add platform"} />
                        }
                    </TreeNode.Options>
                    <TreeNode.ChildNodes>
                        {
                            Object.keys(platformConfigs).map(id =>
                                <PlatformComponent
                                    onDelete={this.onPlatformDelete}
                                    key={id}
                                    id={id}
                                    config={platformConfigs[id]}
                                    type={TaskType.BUILD}
                                    level={1} />
                            )
                        }
                    </TreeNode.ChildNodes>
                </TreeNode>
            </div>
        )
    }
}

export default inject(CONFIG_STORE)(observer(JobConfigComponent));

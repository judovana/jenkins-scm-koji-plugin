import React from "react";
import { PlatformConfig, TaskType, Item } from "../stores/model";
import AddComponent from "./formComponents/AddComponent";
import { inject, observer } from "mobx-react";
import { CONFIG_STORE, ConfigStore } from "../stores/ConfigStore";
import TreeNode from "./TreeNode";
import TaskComponent from "./TaskComponent";
import Button from "./Button";

interface Props {
    config: PlatformConfig;
    type: TaskType;
    configStore?: ConfigStore;
    onDelete: (id: string) => void;
    id: string;
}

class PlatformComponent extends React.PureComponent<Props> {

    onAdd = (id: string): void => {
        this.props.config.tasks[id] = { variants: [] };
    }

    onTaskDelete = (id: string): void => {
        delete this.props.config.tasks[id];
    }

    render() {
        const { configStore, id, config, type, onDelete } = this.props;
        const platform = configStore!.getPlatform(id);
        if (!platform) {
            return <div>unknown platform</div>
        }
        const tasks = configStore!.tasks;
        const taskConfigs = config.tasks;
        const taskConfigIds = Object.keys(taskConfigs);
        const unselectedTasks = tasks
            .filter(task => task.type === type && !taskConfigIds.includes(task.id));
        return (
            <div>
                <TreeNode>
                    <TreeNode.Title>
                        {platform.id}
                    </TreeNode.Title>
                    <TreeNode.NodeInfo>
                        Tasks ({taskConfigIds.length})
                    </TreeNode.NodeInfo>
                    <TreeNode.Options>
                        {[
                            unselectedTasks.length !== 0 && <AddComponent
                                key="add"
                                onAdd={this.onAdd}
                                items={unselectedTasks as Item[]}
                                label={"Add task"} />,
                            <Button
                                key="remove"
                                color="red"
                                onClick={() => onDelete(platform.id)}>Remove</Button>
                        ]}
                    </TreeNode.Options>
                    <TreeNode.ChildNodes>
                        {
                            taskConfigIds.map(id =>
                                <TaskComponent
                                    key={id}
                                    onDelete={this.onTaskDelete}
                                    id={id}
                                    config={taskConfigs[id]} />
                            )
                        }
                    </TreeNode.ChildNodes>
                </TreeNode>
            </div>
        )
    }
}

export default inject(CONFIG_STORE)(observer(PlatformComponent));

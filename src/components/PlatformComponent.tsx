import React from "react";
import { PlatformConfig, TaskType, Item } from "../stores/model";
import AddComponent from "./AddComponent";
import { inject, observer } from "mobx-react";
import { CONFIG_STORE, ConfigStore } from "../stores/ConfigStore";
import TreeNode from "./TreeNode";
import TaskComponent from "./TaskComponent";

interface Props {
    config: PlatformConfig;
    type: TaskType;
    configStore?: ConfigStore;
    onDelete: (id: string) => void;
    id: string;
    level: number;
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
        const platform = configStore!.platforms.get(id);
        if (!platform) {
            return <div>unknown platform</div>
        }
        const tasks = configStore!.tasks;
        const taskConfigs = config.tasks;
        const unselectedTasks = Array.from(tasks.values())
            .filter(task => task.type === type && !Object.keys(taskConfigs).includes(task.id));
        return (
            <div>
                <TreeNode level={this.props.level + 1}>
                    <TreeNode.Title level={this.props.level + 1}>
                        {platform.id}
                    </TreeNode.Title>
                    <TreeNode.NodeInfo>
                        Tasks ({Object.keys(taskConfigs).length})
                    </TreeNode.NodeInfo>
                    <TreeNode.Options>
                        {[
                            unselectedTasks.length !== 0 && <AddComponent
                                key="add"
                                onAdd={this.onAdd}
                                items={unselectedTasks as Item[]}
                                label={"Add task"} />,
                            <button
                                key="remove"
                                style={{ display: "block", justifySelf: "flex-end" }}
                                className="Remove"
                                onClick={() => onDelete(platform.id)}>X</button>
                        ]}
                    </TreeNode.Options>
                    <TreeNode.ChildNodes>
                        {
                            Object.keys(taskConfigs).map(id =>
                                <TaskComponent
                                    key={id}
                                    onDelete={this.onTaskDelete}
                                    id={id}
                                    config={taskConfigs[id]}
                                    level={this.props.level + 1} />
                            )
                        }
                    </TreeNode.ChildNodes>
                </TreeNode>
            </div>
        )
    }
}

export default inject(CONFIG_STORE)(observer(PlatformComponent));

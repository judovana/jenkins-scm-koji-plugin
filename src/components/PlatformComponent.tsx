import React from "react";
import { PlatformConfig, TaskConfig, TaskType, Item } from "../stores/model";
import TaskComponent from "./TaskComponent";
import AddComponent from "./AddComponent";
import { inject, observer } from "mobx-react";
import { CONFIG_STORE, ConfigStore } from "../stores/ConfigStore";

interface Props {
    onChange: (config: PlatformConfig) => void;
    onDelete: (id: string) => void;
    config: PlatformConfig;
    type: TaskType;
    configStore?: ConfigStore;
    id: string;
}

class PlatformComponent extends React.PureComponent<Props> {

    handleTaskChange = (id: string, task: TaskConfig = { variants: [] }): void => {
        const { onChange, config } = this.props;
        onChange({
            ...config,
            tasks: {
                ...config.tasks,
                [id]: task
            }
        });
    }

    handleTaskDeletion = (id: string): void => {
        const config = { ...this.props.config };
        delete config.tasks[id];
        this.props.onChange(config);
    }

    render() {
        const { configStore, id, config, type } = this.props;
        if (!configStore) {
            return null;
        }
        const platform = configStore.platforms.get(id);
        if (!platform) {
            return <div>unknown platform</div>
        }
        const tasks = configStore.tasks;
        const taskConfigs = config.tasks;
        const unselectedTasks = Array.from(tasks.values())
            .filter(task => task.type === type && !Object.keys(taskConfigs).includes(task.id));
        return (
            <div style={container}>
                <div style={{ display: "flex", flexDirection: "row" }}>
                    {platform.id}
                    {
                        unselectedTasks.length === 0 ? null :
                            <AddComponent
                                onAdd={this.handleTaskChange}
                                items={unselectedTasks as Item[]}
                                label={"Add task"} />
                    }
                    <button onClick={() => this.props.onDelete(platform.id)}>X</button>
                </div>
                {
                    Object.keys(taskConfigs).map(id => {
                        return (
                            <div key={id}>
                                <TaskComponent
                                    onChange={config => this.handleTaskChange(id, config)}
                                    onDelete={this.handleTaskDeletion}
                                    id={id}
                                    config={taskConfigs[id]} />
                            </div>
                        )
                    })
                }
            </div>
        );
    }
}

export default inject(CONFIG_STORE)(observer(PlatformComponent));

const container = {
    paddingLeft: 20
}

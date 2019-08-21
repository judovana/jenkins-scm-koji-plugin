import React from "react";
import { PlatformConfig, Platform, TaskConfig, Task, TaskType } from "../store/types";
import TaskComponent from "./TaskComponent";
import { AppState } from "../store/reducer";
import { connect } from "react-redux";
import AddComponent from "./AddComponent";

interface Props {
    onChange: (config: PlatformConfig) => void;
    onDelete: (id: string) => void;
    config: PlatformConfig;
    platform: Platform;
    type: TaskType;
}

interface StateProps {
    tasks: { [id: string]: Task }
}

class PlatformComponent extends React.PureComponent<Props & StateProps> {

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
        const taskConfigs = this.props.config.tasks;
        const unselectedTasks = Object.values(this.props.tasks)
            .filter(task => task.type === this.props.type && !Object.keys(taskConfigs).includes(task.id));
        return (
            <div style={container}>
                <div style={{ display: "flex", flexDirection: "row" }}>
                    {this.props.platform.label}
                    {
                        unselectedTasks.length === 0 ? null :
                            <AddComponent
                                onAdd={this.handleTaskChange}
                                items={unselectedTasks}
                                label={"Add task"} />
                    }
                    <button onClick={() => this.props.onDelete(this.props.platform.id)}>X</button>
                </div>
                {
                    Object.keys(taskConfigs).map(id =>
                        <div key={id}>
                            <TaskComponent
                                onChange={(config) => this.handleTaskChange(id, config)}
                                onDelete={this.handleTaskDeletion}
                                id={id}
                                task={this.props.tasks[id]}
                                config={taskConfigs[id]} />
                        </div>
                    )
                }
            </div>
        );
    }
}

const mapStateToProps = (state: AppState): StateProps => ({
    tasks: state.configs.tasks
})

export default connect(mapStateToProps)(PlatformComponent);

const container = {
    paddingLeft: 20
}

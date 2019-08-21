import React from "react";
import { TaskConfig, TaskVariantCategory, VariantsConfig, Task } from "../store/types";
import VariantComponent from "./VariantComponent";
import { AppState } from "../store/reducer";
import { connect } from "react-redux";

interface Props {
    id: string;
    config: TaskConfig;
    task: Task;
    onDelete: (id: string) => void;
    onChange: (config: TaskConfig) => void;
}

interface StateProps {
    categories: TaskVariantCategory[];

}

class TaskComponent extends React.PureComponent<Props & StateProps> {

    handleVariantChange = (index: number, config: VariantsConfig): void => {
        const { onChange, config: task } = this.props;
        onChange({
            ...task,
            variants: [
                ...task.variants.splice(0, index),
                config,
                ...task.variants.splice(index + 1)
            ]
        });
    }

    onAdd = (): void => {
        const variants = [...this.props.config.variants];
        variants.push({ map: {} });
        this.props.onChange({
            ...this.props.config,
            variants
        });
    }

    onVariantDeletion = (index: number): void => {
        const variants = [...this.props.config.variants];
        variants.splice(index, 1);
        this.props.onChange({
            ...this.props.config,
            variants
        })
    }

    render() {
        return (
            <div style={container}>
                <div style={{ display: "flex", flexDirection: "row" }}>
                    {this.props.task.label}
                    <button onClick={this.onAdd}>Add variant</button>
                    <button onClick={() => this.props.onDelete(this.props.task.id)}>X</button>
                </div>
                {
                    this.props.config.variants.map((variant, index) =>
                        <div key={index}>
                            <VariantComponent
                                type={this.props.task.type}
                                onChange={(_variant) => this.handleVariantChange(index, _variant)}
                                onDelete={() => this.onVariantDeletion(index)}
                                categories={this.props.categories}
                                variant={variant} />
                        </div>
                    )
                }
            </div>
        );
    }
}

const mapStateToProps = (state: AppState, ownProps: Props): StateProps => {
    const { tasks, taskVariantCategories } = state.configs;
    return {
        categories: Object.values(taskVariantCategories).filter(category => category.type === tasks[ownProps.id].type)
    };
};

export default connect(mapStateToProps)(TaskComponent);

const container: React.CSSProperties = {
    marginLeft: 10
}

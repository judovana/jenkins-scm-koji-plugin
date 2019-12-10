import React from "react"
import { useObserver } from "mobx-react"
import { Tooltip, IconButton, TableCell, TableRow } from "@material-ui/core"
import { Add, Delete } from "@material-ui/icons"

import { TaskConfig, TaskType, ProjectType } from "../../stores/model"

import useStores from "../../hooks/useStores"
import VariantRow from "./VariantRow"

type TaskRowProps = {
    id: string
    config: TaskConfig
    treeID: string
    onDelete: (id: string) => void
    projectType: ProjectType
    type: TaskType
}


const TaskRow: React.FC<TaskRowProps> = props => {

    //     onAdd = (): void => {
    //         this.props.config.variants.push({ map: {} })
    //     }

    //     onVariantDelete = (index: number): void => {
    //         this.props.config.variants.splice(index, 1)
    //     }
    //         const { configStore, id, config, onDelete } = this.props
    //         const task = configStore!.getTask(id)
    //         if (!task) {
    //     return (
    //         <div>uknown task</div>
    //     )
    // }

    const { configStore } = useStores()

    return useObserver(() => {

        const { config, id, treeID, onDelete, projectType, type } = props

        const taskVariants = config.variants

        const onAdd = () => {
            taskVariants.splice(0, 0, {
                map: configStore.taskVariants
                    .filter(taskVariant => taskVariant.type === type)
                    .reduce((map, taskVariant) => {
                        map[taskVariant.id] = taskVariant.defaultValue
                        return map
                    }, {} as { [key: string]: string }),
                platforms: (type === "BUILD" && {}) || undefined
            })
        }

        const cell: JSX.Element = (
            <span>
                {id}
                <Tooltip title={`Add ${type.toLowerCase()} variant`}>
                    <IconButton onClick={onAdd}>
                        <Add />
                    </IconButton>
                </Tooltip>
            </span>
        )

        const buildCols = projectType === "JDK_PROJECT" &&
            <React.Fragment>
                <TableCell></TableCell>
                <TableCell>{type === "BUILD" && cell}</TableCell>
                <TableCell></TableCell>
            </React.Fragment>

        return (
            <React.Fragment>
                <TableRow>
                    {buildCols}
                    <TableCell></TableCell>
                    <TableCell>{type === "TEST" && cell}</TableCell>
                    <TableCell></TableCell>
                    <TableCell>
                        <IconButton onClick={() => { onDelete(id) }}>
                            <Delete />
                        </IconButton>
                    </TableCell>
                </TableRow>
                {taskVariants.map((taskVariant, index) =>
                    <VariantRow
                        config={taskVariant}
                        key={treeID + index}
                        treeID={treeID + index}
                        onDelete={() => taskVariants.splice(index, 1)}
                        projectType={projectType}
                        type={type} />)}
            </React.Fragment>
        )
    })
}

export default TaskRow

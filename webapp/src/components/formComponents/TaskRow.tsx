import React from "react"
import { useObserver } from "mobx-react"
import { Tooltip, IconButton, TableCell, TableRow } from "@material-ui/core"
import { Add } from "@material-ui/icons"

import { TaskConfig, TaskType, ProjectType } from "../../stores/model"

import useStores from "../../hooks/useStores"
import VariantRow from "./VariantRow"
import createTaskVariantsMap from "../../utils/createVariantMap";
import DeleteButton from "../DeleteButton"

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
                map: createTaskVariantsMap(
                    configStore.taskVariants,
                    taskVariant => taskVariant.type === type
                ),
                platforms: (type === "BUILD" && []) || undefined
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

        return (
            <React.Fragment>
                <TableRow>
                    <TableCell></TableCell>
                    {projectType === "JDK_PROJECT" &&
                        <TableCell>{type === "BUILD" && cell}</TableCell>}
                    <TableCell></TableCell>
                    <TableCell></TableCell>
                    <TableCell>{type === "TEST" && cell}</TableCell>
                    <TableCell></TableCell>
                    <TableCell>
                        <DeleteButton onClick={() => { onDelete(id) }} />
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

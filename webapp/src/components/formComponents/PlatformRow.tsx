import React from "react"
import { useObserver } from "mobx-react"
import { TableCell, TableRow, IconButton } from "@material-ui/core"
import { Delete } from "@material-ui/icons"

import { PlatformConfig, TaskType, ProjectType } from "../../stores/model"
import AddComponent from "./AddComponent"
import useStores from "../../hooks/useStores"
import TaskRow from "./TaskRow"

type PlatformRowProps = {
    config: PlatformConfig
    id: string
    treeID: string
    onDelete: (id: string) => void
    projectType: ProjectType
    type: TaskType

}

const PlatformRow: React.FC<PlatformRowProps> = props => {

    const { configStore } = useStores()

    return useObserver(() => {
        const { id, config, treeID, onDelete, projectType, type } = props

        const taskConfigs = config.tasks

        const onTaskDelete = (id: string): void => {
            delete config.tasks[id]
        }

        const unselectedTasks = configStore.tasks
            .filter(task => task.type === type && !taskConfigs[task.id])
        const cell: JSX.Element = (
            <span>
                {id}
                <AddComponent
                    label={`Add ${type.toLowerCase()} task`}
                    items={unselectedTasks}
                    onAdd={(taskId) => { config.tasks[taskId] = { variants: [] } }} />
            </span>
        )

        const buildCols = projectType === "JDK_PROJECT" && <React.Fragment>
            <TableCell>{type === "BUILD" && cell}</TableCell>
            <TableCell></TableCell>
            <TableCell></TableCell>
        </React.Fragment>

        return (
            <React.Fragment>
                <TableRow>
                    {buildCols}
                    <TableCell>{type === "TEST" && cell}</TableCell>
                    <TableCell></TableCell>
                    <TableCell></TableCell>
                    <TableCell>
                        <IconButton onClick={() => onDelete(id)}>
                            <Delete />
                        </IconButton>
                    </TableCell>
                </TableRow>
                {Object.entries(taskConfigs).map(([id, config]) =>
                    <TaskRow
                        config={config}
                        id={id}
                        key={treeID + id}
                        treeID={treeID + id}
                        onDelete={onTaskDelete}
                        projectType={projectType}
                        type={type} />)}
            </React.Fragment>)
    })
}

export default PlatformRow

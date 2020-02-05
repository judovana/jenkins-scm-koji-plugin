import React from "react"
import { useObserver } from "mobx-react"
import { TableCell, TableRow, IconButton, Tooltip } from "@material-ui/core"
import { Delete, Add } from "@material-ui/icons"

import { PlatformConfig, TaskType, ProjectType } from "../../stores/model"
import AddComponent from "./AddComponent"
import useStores from "../../hooks/useStores"
import TaskRow from "./TaskRow"
import VariantRow from "./VariantRow"

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
        const variantConfigs = config.variants

        const onTaskDelete = (id: string): void => {
            delete config.tasks![id]
        }

        const onVariantDelete = (index: number) => {
            config.variants!.splice(index, 1)
        }

        const onTaskAdd = (id: string) => {
            config.tasks![id] = { variants: [] }
        }

        const onVariantAdd = () => {
            config.variants!.splice(0, 0, {
                map: configStore.taskVariants
                    .filter(taskVariant => taskVariant.type === type)
                    .reduce((map, taskVariant) => {
                        map[taskVariant.id] = taskVariant.defaultValue
                        return map
                    }, {} as { [key: string]: string }),
                platforms: (type === "BUILD" && {}) || undefined
            })
        }

        const unselectedTasks = taskConfigs && configStore.tasks
            .filter(task => task.type === type && !taskConfigs[task.id])

        const unselectedPlatforms = variantConfigs && configStore.taskVariants
            .filter(variant => variant.type === type)

        const cell: JSX.Element = (
            <span>
                {id}
                {(
                    taskConfigs && (<AddComponent
                        label={`Add ${type.toLowerCase()} ${(taskConfigs && "task") || (variantConfigs && "variant")}`}
                        items={unselectedTasks || unselectedPlatforms || []}
                        onAdd={(taskConfigs && onTaskAdd) || (variantConfigs && onVariantAdd) || (() => null)} />)
                ) || (
                        variantConfigs && (<Tooltip title={`Add ${type.toLowerCase()} variant`}>
                            <IconButton onClick={onVariantAdd}>
                                <Add />
                            </IconButton>
                        </Tooltip>)
                    )}
            </span>
        )

        return (
            <React.Fragment>
                <TableRow>
                    <TableCell>{type === "BUILD" && cell}</TableCell>
                    {projectType === "JDK_PROJECT" &&
                        <TableCell></TableCell>}
                    <TableCell></TableCell>
                    <TableCell>{type === "TEST" && cell}</TableCell>
                    <TableCell></TableCell>
                    <TableCell></TableCell>
                    <TableCell>
                        <IconButton onClick={() => onDelete(id)}>
                            <Delete />
                        </IconButton>
                    </TableCell>
                </TableRow>
                {(taskConfigs && Object.entries(taskConfigs).map(([id, config]) =>
                    <TaskRow
                        config={config}
                        id={id}
                        key={treeID + id}
                        treeID={treeID + id}
                        onDelete={onTaskDelete}
                        projectType={projectType}
                        type={type} />))
                    ||
                    (variantConfigs && variantConfigs.map((config, index) =>
                        <VariantRow
                            config={config}
                            key={treeID + id}
                            onDelete={() => onVariantDelete(index)}
                            projectType={projectType}
                            treeID={treeID + id}
                            type={type} />
                    ))}
            </React.Fragment>)
    })
}

export default PlatformRow

import React from "react"
import { useObserver } from "mobx-react"
import { TableCell, TableRow, IconButton, Tooltip } from "@material-ui/core"
import { Delete, Add } from "@material-ui/icons"

import { PlatformConfig, TaskType, ProjectType } from "../../stores/model"
import AddComponent from "./AddComponent"
import useStores from "../../hooks/useStores"
import TaskRow from "./TaskRow"
import VariantRow from "./VariantRow"
import Select from "./Select"
import createTaskVariantsMap from "../../utils/createVariantMap";

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
    const platform = configStore.getPlatform(props.id)

    React.useEffect(() => {
        if (platform && props.config.tasks) {
            const provider = platform.providers.find(_ => true)
            props.config.provider = provider && provider.id
        }
    }, [platform, props.config.tasks, props.config.provider])

    return useObserver(() => {
        const { id, config, treeID, onDelete, projectType, type } = props

        const taskConfigs = config.tasks
        const variantConfigs = config.variants

        if (!platform) {
            return null
        }

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
                map: createTaskVariantsMap(
                    configStore.taskVariants,
                    taskVariant => taskVariant.type === type && taskVariant.supportsSubpackages
                ),
                platforms: (type === "BUILD" && {}) || undefined
            })
        }

        const unselectedTasks = taskConfigs && configStore.tasks
            .filter(task => task.type === type && !taskConfigs[task.id])

        const cell: JSX.Element = (
            <span>
                {id}
                {(
                    taskConfigs && (<React.Fragment>
                        {/* This will uglify the form, TODO: pretify it  */}
                        <Select
                            label="Provider"
                            onChange={value => config.provider = value}
                            options={platform.providers.map(provider => provider.id)}
                            value={config.provider}/>
                        <AddComponent
                            label={`Add ${type.toLowerCase()} task`}
                            items={unselectedTasks || []}
                            onAdd={onTaskAdd} />
                    </React.Fragment>)
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

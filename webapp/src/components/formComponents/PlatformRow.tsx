import React from "react"
import { useObserver } from "mobx-react"
import { TableRow, IconButton, Tooltip } from "@material-ui/core"
import { Add } from "@material-ui/icons"

import {
    BuildConfigs,
    PlatformConfig,
    ProjectType,
    TaskType,
} from "../../stores/model"
import AddComponent from "./AddComponent"
import useStores from "../../hooks/useStores"
import TaskRow from "./TaskRow"
import VariantRow from "./VariantRow"
import Select from "./Select"
import createTaskVariantsMap from "../../utils/createVariantMap";
import DeleteButton from "../DeleteButton"
import TableCell from "../TableCell"

type PlatformRowProps = {
    buildConfigs?: BuildConfigs
    config: PlatformConfig
    jdkId: string
    onDelete: () => void
    projectId: string
    projectType: ProjectType
    treeID: string
    type: TaskType
}

const PlatformRow: React.FC<PlatformRowProps> = props => {

    const { configStore } = useStores()

    const platform = configStore.getPlatform(props.config.id)

    React.useEffect(() => {
        if (platform && props.config.tasks && !props.config.provider) {
            const provider = platform.providers.find(_ => true)
            props.config.provider = provider && provider.id
        }
    }, [platform, props.config.tasks, props.config.provider, props.config])

    return useObserver(() => {
        const {
            buildConfigs,
            config,
            jdkId,
            treeID,
            onDelete,
            projectId,
            projectType,
            type,
        } = props
        const { id, tasks: taskConfigs, variants: variantConfigs } = config

        if (!platform) {
            return null
        }
        const onTaskDelete = (index: number): void => {
            config.tasks!.splice(index, 1)
        }

        const onVariantDelete = (index: number) => {
            config.variants!.splice(index, 1)
        }

        const onTaskAdd = (id: string) => {
            config.tasks!.push({ id, variants: [] })
        }

        const onVariantAdd = () => {
            config.variants!.splice(0, 0, {
                map: createTaskVariantsMap(
                    configStore.taskVariants,
                    taskVariant => taskVariant.type === type && taskVariant.supportsSubpackages
                ),
                platforms: (type === "BUILD" && []) || undefined
            })
        }

        const unselectedTasks = taskConfigs && configStore.tasks
            .filter(task => task.type === type && !taskConfigs.find(_task => task.id === _task.id))

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
                        <DeleteButton onClick={onDelete} />
                    </TableCell>
                </TableRow>
                {(taskConfigs && taskConfigs.map((taskConfig, index) =>
                    <TaskRow
                        buildConfigs={buildConfigs}
                        config={taskConfig}
                        jdkId={jdkId}
                        key={treeID + config.id}
                        treeID={treeID + config.id}
                        onDelete={() => onTaskDelete(index)}
                        platformConfig={config}
                        projectId={projectId}
                        projectType={projectType}
                        type={type} />))
                    ||
                    (variantConfigs && variantConfigs.map((variantsConfig, index) =>
                        <VariantRow
                            buildConfigs={buildConfigs}
                            config={variantsConfig}
                            jdkId={jdkId}
                            key={treeID + id + index}
                            onDelete={() => onVariantDelete(index)}
                            platformConfig={config}
                            projectId={projectId}
                            projectType={projectType}
                            taskConfig={undefined}
                            treeID={treeID + id}
                            type={type} />
                    ))}
            </React.Fragment>)
    })
}

export default PlatformRow

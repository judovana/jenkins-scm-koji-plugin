import React from "react"
import { JobConfig, PlatformConfig, VariantsConfig, TaskConfig, TaskType } from "../../stores/model"
import AddComponent from "./AddComponent"
import { ConfigStore, CONFIG_STORE } from "../../stores/ConfigStore"
import { inject, observer } from "mobx-react"
import { Table, TableHead, TableRow, TableCell, TableBody, Tooltip, IconButton } from "@material-ui/core"
import Select from "./Select"
import { Add, Delete } from "@material-ui/icons"

interface Props {
    jobConfig: JobConfig
    configStore?: ConfigStore
    type?: TaskType
}

class JobConfigComponent extends React.PureComponent<Props> {

    onPlatformAdd = (id: string, platformConfig: PlatformConfig = { tasks: {} }): void => {
        this.props.jobConfig.platforms[id] = platformConfig
    }

    onPlatformDelete = (id: string): void => {
        delete this.props.jobConfig.platforms[id]
    }

    platformRows = (key: string, platformConfigs: { [id: string]: PlatformConfig }, type: TaskType): JSX.Element[] => {
        const configStore = this.props.configStore!
        return Object.keys(platformConfigs).flatMap(id => {
            const platformConfig = platformConfigs[id]
            const taskConfigs = platformConfig.tasks

            const unselectedTasks = configStore.tasks
                .filter(task => task.type === type && !taskConfigs[task.id])
            const cell: JSX.Element = (
                <span>
                    {id}
                    <AddComponent
                        label={`Add ${type.toLowerCase()} task`}
                        items={unselectedTasks}
                        onAdd={(taskId) => { platformConfig.tasks[taskId] = { variants: [] } }} />
                </span>
            )

            const buildCols = this.props.type === "BUILD" && [
                <TableCell key={0}>{type === "BUILD" && cell}</TableCell>,
                <TableCell key={1}></TableCell>,
                <TableCell key={2}></TableCell>
            ]

            return [
                <TableRow key={key + id}>
                    {buildCols}
                    <TableCell>{type === "TEST" && cell}</TableCell>
                    <TableCell></TableCell>
                    <TableCell></TableCell>
                    <TableCell>
                        <IconButton onClick={() => { delete platformConfigs[id] }}>
                            <Delete />
                        </IconButton>
                    </TableCell>
                </TableRow>,
                ...this.taskRows(key + id, taskConfigs, type)
            ]
        })
    }

    taskRows = (key: string, taskConfigs: { [id: string]: TaskConfig }, type: TaskType): JSX.Element[] => {
        const configStore = this.props.configStore!
        return Object.keys(taskConfigs).flatMap(id => {
            const taskConfig = taskConfigs[id]
            const taskVariants = taskConfig.variants

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

            const buildCols = this.props.type === "BUILD" &&
                [
                    <TableCell key={0}></TableCell>,
                    <TableCell key={1}>{type === "BUILD" && cell}</TableCell>,
                    <TableCell key={2}></TableCell>
                ]

            return [
                <TableRow key={key + id}>
                    {buildCols}
                    <TableCell></TableCell>
                    <TableCell>{type === "TEST" && cell}</TableCell>
                    <TableCell></TableCell>
                    <TableCell>
                        <IconButton onClick={() => { delete taskConfigs[id] }}>
                            <Delete />
                        </IconButton>
                    </TableCell>
                </TableRow>,
                ...this.taskVariantRows(key + id, taskVariants, type)
            ]
        })
    }

    taskVariantRows = (key: string, variantConfigs: VariantsConfig[], type: TaskType): JSX.Element[] => {
        const configStore = this.props.configStore!
        const testVariantRows: JSX.Element[] = variantConfigs.flatMap((variant, index) => {
            const taskVariants = configStore!.taskVariants.filter(variant => variant.type === type)

            const unselectedPlatforms = variant.platforms && type === "BUILD" && configStore.platforms
                .filter(platform => !variant.platforms![platform.id])

            const cell = (
                <div style={{ display: "flex", flexDirection: "row" }}>
                    {
                        taskVariants.map(taskVariant =>
                            <Select
                                options={Object.values(taskVariant.variants).map(variant => variant.id)}
                                label={taskVariant.id}
                                value={variant.map[taskVariant.id]}
                                onChange={(value: string) => { variant.map[taskVariant.id] = value }}
                                key={taskVariant.id} />
                        )
                    }
                    {
                        unselectedPlatforms && <AddComponent
                            label={`Add test platform`}
                            items={unselectedPlatforms}
                            onAdd={(taskId) => {
                                if (!variant.platforms) {
                                    variant.platforms = {}
                                }
                                variant.platforms[taskId] = { tasks: {} }
                            }} />
                    }
                </div>
            )

            const platformRows = (type === "BUILD" && variant.platforms && this.platformRows(key + index, variant.platforms, "TEST")) || []

            const buildCols = this.props.type === "BUILD" && [
                <TableCell key={0}></TableCell>,
                <TableCell key={1}></TableCell>,
                <TableCell key={2}>{type === "BUILD" && cell}</TableCell>
            ]

            const testVariantRow: JSX.Element[] = [
                <TableRow key={key + index}>
                    {buildCols}
                    <TableCell></TableCell>
                    <TableCell></TableCell>
                    <TableCell>{type === "TEST" && cell}</TableCell>
                    <TableCell>
                        <IconButton onClick={() => { variantConfigs.splice(index, 1) }}>
                            <Delete />
                        </IconButton>
                    </TableCell>
                </TableRow>,
                ...platformRows
            ]

            return testVariantRow
        })
        return testVariantRows
    }

    render() {

        const { configStore, jobConfig } = this.props
        const platformConfigs = jobConfig.platforms

        const unselectedPlatforms = configStore!.platforms
            .filter(platform => !platformConfigs[platform.id])

        const type = this.props.type || "BUILD"
        const typePretty = type.toLowerCase()
        return (
            <div>
                <Table stickyHeader>
                    <TableHead>
                        <TableRow>
                            <TableCell>
                                <span>
                                    {`${typePretty} platform`}
                                    <AddComponent
                                        label={`Add ${typePretty} platform`}
                                        items={unselectedPlatforms}
                                        onAdd={(platformId) => {
                                            platformConfigs[platformId] = { tasks: {} }
                                        }} />
                                </span>
                            </TableCell>
                            <TableCell>{`${typePretty} tasks`}</TableCell>
                            <TableCell>{`${typePretty} variants`}</TableCell>
                            {
                                type === "BUILD" && [
                                    <TableCell key={0}>test platform</TableCell>,
                                    <TableCell key={1}>test task</TableCell>,
                                    <TableCell key={2}>test variants</TableCell>
                                ]
                            }
                            <TableCell></TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {
                            this.platformRows("job", platformConfigs, type)
                        }
                    </TableBody>
                </Table>
            </div>
        )
    }
}

export default inject(CONFIG_STORE)(observer(JobConfigComponent))

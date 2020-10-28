import React from "react"
import { useObserver } from "mobx-react"
import { TableRow } from "@material-ui/core"

import {
    BuildConfigs,
    VariantsConfig,
    TaskType,
    ProjectType,
    PlatformConfig,
    TaskConfig,
} from "../../stores/model"
import Select from "./Select"
import AddComponent from "./AddComponent"
import useStores from "../../hooks/useStores"
import PlatformRow from "./PlatformRow"
import DeleteButton from "../DeleteButton"
import TableCell from "../TableCell"

type VariantRowProps = {
    buildConfigs: BuildConfigs | undefined
    config: VariantsConfig
    jdkId: string
    onDelete: () => void
    projectType: ProjectType
    platformConfig: PlatformConfig
    projectId: string
    taskConfig: TaskConfig | undefined
    treeID: string
    type: TaskType
}

const VariantRow: React.FC<VariantRowProps> = props => {

    const { configStore } = useStores()

    return useObserver(() => {

        const {
            buildConfigs,
            config,
            jdkId,
            treeID,
            onDelete,
            platformConfig,
            projectId,
            projectType,
            taskConfig,
            type,
        } = props
        const { taskVariantsMap, platforms } = configStore

        const onPlatformDelete = (index: number): void => {
            config.platforms!.splice(index, 1)
        }
        const selectedPlatformsIds =
            config.platforms &&
            type === "BUILD" &&
            config.platforms.map(platform => platform.id)

        const sortedVariants = Object.entries(config.map)
            .slice() // .sort sorts in place so we need to make a copy to stay pure
            .sort(([k1], [k2]) => k1.localeCompare(k2))

        const jenkinsJobLink = configStore.jobNameGenerator(projectId, jdkId, platformConfig, taskConfig, config, buildConfigs)
        const cell = (<div style={{ display: "flex", flexDirection: "column" }}>
            <div style={{ display: "flex", flexDirection: "row" }}>
                {sortedVariants.map(([id, value]) => {
                    const taskVariant = taskVariantsMap[id]
                    return (
                        <Select
                            options={Object.values(taskVariant.variants).map(
                                variant => variant.id
                            )}
                            label={id}
                            value={value || taskVariant.defaultValue}
                            onChange={(value: string) => {
                                config.map[id] = value
                            }}
                            key={id}
                        />
                    )
                })}
                {
                    selectedPlatformsIds && <AddComponent
                        label={`Add test platform`}
                        items={platforms
                            .map(({id}) => ({
                                id,
                                marked: selectedPlatformsIds.includes(id)
                            }))}
                        onAdd={id => {
                            if (!config.platforms) {
                                config.platforms = []
                            }
                            config.platforms.push({ id, tasks: [] })
                        }} />
                }
            </div>
            {!!jenkinsJobLink && <a style={{ alignSelf: "flex-end" }} href={jenkinsJobLink}>{"jenkins job"}</a>}
        </div>)

        return (
            <React.Fragment>
                <TableRow>
                    <TableCell></TableCell>
                    {projectType === "JDK_PROJECT" &&
                        <TableCell></TableCell>}
                    <TableCell>{type === "BUILD" && cell}</TableCell>
                    <TableCell></TableCell>
                    <TableCell></TableCell>
                    <TableCell>{type === "TEST" && cell}</TableCell>
                    <TableCell>
                        <DeleteButton onClick={onDelete} />
                    </TableCell>
                </TableRow>
                {type === "BUILD" && config.platforms && config.platforms
                    .map((testPlatformConfig, index) =>
                        <PlatformRow
                            buildConfigs={{
                                platform: platformConfig,
                                task: taskConfig,
                                taskVariants: config,
                            }}
                            config={testPlatformConfig}
                            jdkId={jdkId}
                            key={treeID + testPlatformConfig.id}
                            treeID={treeID + testPlatformConfig.id}
                            onDelete={() => onPlatformDelete(index)}
                            projectId={projectId}
                            projectType={projectType}
                            type="TEST"
                        />)}
            </React.Fragment>
        )
    })
}

export default VariantRow

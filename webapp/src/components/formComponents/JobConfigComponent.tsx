import React from "react"
import { useObserver } from "mobx-react"

import { JobConfig, ProjectType } from "../../stores/model"
import AddComponent from "./AddComponent"
import { Table, TableHead, TableRow, TableBody } from "@material-ui/core"
import useStores from "../../hooks/useStores"
import PlatformRow from "./PlatformRow"
import TableCell from "../TableCell"


interface Props {
    jobConfig: JobConfig
    projectType: ProjectType
}

const JobConfigComponent: React.FC<Props> = props => {

    const { configStore } = useStores()

    return useObserver(() => {
        const { jobConfig, projectType } = props
        const platformConfigs = jobConfig.platforms
        const onPlatformDelete = (index: number): void => {
            platformConfigs.splice(index, 1)
        }

        const onPlatformAdd = (id: string) => {
            if (projectType === "JDK_PROJECT") {
                platformConfigs.push({ id, tasks: {} })
            } else if (projectType === "JDK_TEST_PROJECT") {
                platformConfigs.push({ id, variants: [] })
            }
        }
        const selectedPlatformsIds = platformConfigs.map(platform => platform.id)
        const { platforms } = configStore

        return (
            <div>
                <Table stickyHeader>
                    <TableHead>
                        <TableRow>
                            <TableCell>
                                <span>
                                    {`build platform`}
                                    <AddComponent
                                        label={`Add build platform`}
                                        items={platforms
                                            .map(({id}) => ({
                                                id,
                                                marked: selectedPlatformsIds.includes(id)
                                            }))}
                                        onAdd={onPlatformAdd} />
                                </span>
                            </TableCell>
                            {projectType === "JDK_PROJECT" && <TableCell>build tasks</TableCell>}
                            <TableCell>build variants</TableCell>
                            <TableCell>test platform</TableCell>
                            <TableCell>test task</TableCell>
                            <TableCell>test variants</TableCell>
                            <TableCell></TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {platformConfigs.map((config, index) =>
                            <PlatformRow
                                config={config}
                                key={index}
                                treeID={index.toString()}
                                onDelete={() => onPlatformDelete(index)}
                                projectType={projectType}
                                type={"BUILD"}
                            />)}
                    </TableBody>
                </Table>
            </div>
        )
    })
}

export default JobConfigComponent

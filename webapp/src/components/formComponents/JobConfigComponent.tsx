import React from "react"
import { useObserver } from "mobx-react"

import { JobConfig, ProjectType } from "../../stores/model"
import AddComponent from "./AddComponent"
import { Table, TableHead, TableRow, TableCell, TableBody } from "@material-ui/core"
import useStores from "../../hooks/useStores"
import PlatformRow from "./PlatformRow"


interface Props {
    jobConfig: JobConfig
    projectType: ProjectType
}

const JobConfigComponent: React.FC<Props> = props => {

    const { configStore } = useStores()

    return useObserver(() => {
        const { jobConfig, projectType } = props

        const onPlatformDelete = (id: string): void => {
            delete jobConfig.platforms[id]
        }

        const onPlatformAdd = (id: string) => {
            if (projectType === "JDK_PROJECT") {
                platformConfigs[id] = { tasks: {} }
            } else if (projectType === "JDK_TEST_PROJECT") {
                platformConfigs[id] = { variants: [] }
            }
        }

        const platformConfigs = jobConfig.platforms

        const unselectedPlatforms = configStore!.platforms
            .filter(platform => !platformConfigs[platform.id])

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
                                        items={unselectedPlatforms}
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
                        {Object.entries(platformConfigs).flatMap(([id, config]) =>
                            <PlatformRow
                                config={config}
                                id={id}
                                key={id}
                                treeID={id}
                                onDelete={onPlatformDelete}
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

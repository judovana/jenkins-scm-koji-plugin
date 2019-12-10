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
        const type = projectType === "JDK_PROJECT" ? "BUILD" : "TEST"

        const onPlatformDelete = (id: string): void => {
            delete jobConfig.platforms[id]
        }

        const platformConfigs = jobConfig.platforms

        const unselectedPlatforms = configStore!.platforms
            .filter(platform => !platformConfigs[platform.id])

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
                                projectType === "JDK_PROJECT" && <React.Fragment>
                                    <TableCell>test platform</TableCell>
                                    <TableCell>test task</TableCell>
                                    <TableCell>test variants</TableCell>
                                </React.Fragment>
                            }
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
                                type={type}
                            />)}
                    </TableBody>
                </Table>
            </div>
        )
    })
}

export default JobConfigComponent

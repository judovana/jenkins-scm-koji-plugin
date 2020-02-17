import React from "react"
import { useObserver } from "mobx-react"
import {
    IconButton,
    Grid,
    Paper,
    Typography,
    Table,
    TableRow,
    TableCell,
    TableBody,
    TableHead
} from "@material-ui/core"
import { Delete, Add } from "@material-ui/icons"
import useStores from "../hooks/useStores"

const List: React.FC = () => {
    const { configStore, viewStore } = useStores()

    return useObserver(() => {
        const {
            configGroupMap,
            deleteConfig,
            selectedConfigGroupId
        } = configStore
        const { goToConfigEditForm, goToConfigNewForm } = viewStore

        if (!selectedConfigGroupId) {
            return <div>{`Ooops`}</div>
        }

        const displayedGroup = configGroupMap[selectedConfigGroupId]

        if (!displayedGroup) {
            return <div>{`Unknown configs: ${selectedConfigGroupId}`}</div>
        }
        return (
            <React.Fragment>
                <Grid
                    alignItems="center"
                    container
                    item
                    justify="center"
                    xs={12}>
                    <Paper style={{ padding: 20, width: "100%" }}>
                        <Grid
                            alignItems="center"
                            container
                            direction="row"
                            justify="space-between">
                            <Typography variant="h4">
                                {selectedConfigGroupId}
                            </Typography>
                            <IconButton
                                onClick={() =>
                                    goToConfigNewForm(selectedConfigGroupId)
                                }>
                                <Add />
                            </IconButton>
                        </Grid>
                        <Table size="small">
                            <TableHead>
                                <TableRow>
                                    <TableCell>Name</TableCell>
                                    <TableCell></TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {Object.values(displayedGroup.configs).map(
                                    ({ id }, index) => (
                                        <TableRow
                                            key={index}
                                            onClick={_ =>
                                                goToConfigEditForm(
                                                    selectedConfigGroupId,
                                                    id
                                                )
                                            }>
                                            <TableCell>{id}</TableCell>
                                            <TableCell align="right">
                                                <IconButton
                                                    onClick={e => {
                                                        e.stopPropagation()
                                                        deleteConfig(
                                                            selectedConfigGroupId,
                                                            id
                                                        )
                                                    }}>
                                                    <Delete />
                                                </IconButton>
                                            </TableCell>
                                        </TableRow>
                                    )
                                )}
                            </TableBody>
                        </Table>
                    </Paper>
                </Grid>
            </React.Fragment>
        )
    })
}

export default List

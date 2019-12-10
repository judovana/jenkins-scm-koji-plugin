import React from "react"
import { useObserver } from "mobx-react"
import { Item } from "../stores/model"
import { IconButton, Grid, Paper, Typography, Table, TableRow, TableCell, TableBody, TableHead } from "@material-ui/core"
import { Delete, Add } from "@material-ui/icons"
import { useHistory } from "react-router-dom"
import useStores from "../hooks/useStores"

const List: React.FC = () => {

    const history = useHistory()

    const { configStore } = useStores()

    const onCreate = () => {
        history.push(`/form/${configStore.selectedGroupId}/`)
    }

    const onDelete = (config: Item) => (event: React.MouseEvent<HTMLButtonElement, MouseEvent>): void => {
        event.stopPropagation()
        configStore.deleteConfig(config.id)
    }

    const onSelect = (config: Item) => (_: React.MouseEvent<HTMLTableRowElement, MouseEvent>): void => {
        history.push(`/form/${configStore.selectedGroupId}/${config.id}`)
        configStore.selectConfig(config)
    }

    return useObserver(() => {
        const { selectedGroup, selectedGroupId } = configStore

        return (
            <React.Fragment>
                <Grid alignItems="center" container item justify="center" xs={12}>
                    <Paper style={{ padding: 20, width: "100%" }}>
                        <Grid alignItems="center" container direction="row" justify="space-between">
                            <Typography variant="h4">{selectedGroupId}</Typography>
                            <IconButton onClick={onCreate}>
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
                                {
                                    selectedGroup.map((config, index) =>
                                        <TableRow
                                            key={index}
                                            onClick={onSelect(config)}>
                                            <TableCell>
                                                {config.id}
                                            </TableCell>
                                            <TableCell align="right">
                                                <IconButton onClick={onDelete(config)}>
                                                    <Delete />
                                                </IconButton>
                                            </TableCell>
                                        </TableRow>
                                    )
                                }
                            </TableBody>
                        </Table>
                    </Paper>
                </Grid>
            </React.Fragment>
        )
    })
}

export default List

import React from "react"
import {
    Dialog,
    DialogTitle,
    DialogContent,
    Table,
    TableHead,
    TableRow,
    TableCell,
    TableBody
} from "@material-ui/core"
import { useObserver } from "mobx-react"

import useStores from "../hooks/useStores"
import { JobUpdateResult } from "../stores/model"

interface ResultDialogProps {}

const ResultDialog: React.FC<ResultDialogProps> = () => {
    const { configStore } = useStores()
    return useObserver(() => {
        const {
            closeResultDialog,
            resultDialogOpen,
            jobUpdateResults
        } = configStore
        if (!jobUpdateResults) {
            return null
        }
        const {
            jobsArchived,
            jobsCreated,
            jobsRevived,
            jobsRewritten
        } = jobUpdateResults
        return (
            <Dialog
                fullWidth
                maxWidth="lg"
                onClose={closeResultDialog}
                open={resultDialogOpen}
                scroll="body">
                <DialogTitle>Modified jobs</DialogTitle>
                <DialogContent dividers>
                    <Table>
                        <TableHead>
                            <TableRow>
                                <TableCell>Action</TableCell>
                                <TableCell>Job name</TableCell>
                                <TableCell>Message</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            <ResultRows action="create" results={jobsCreated} />
                            <ResultRows action="rewrite" results={jobsRewritten} />
                            <ResultRows action="revive" results={jobsRevived} />
                            <ResultRows action="archive" results={jobsArchived} />
                        </TableBody>
                    </Table>
                </DialogContent>
            </Dialog>
        )
    })
}

interface ResultRowsProps {
    action: string
    results: JobUpdateResult[]
}

const ResultRows: React.FC<ResultRowsProps> = ({ action, results }) => (
    <React.Fragment>
        {results.map(({ jobName, message, success }, index) => (
            <TableRow
                key={index}
                style={{
                    borderLeftWidth: 3,
                    borderLeftStyle: "solid",
                    borderLeftColor: success ? "green" : "red"
                }}>
                <TableCell>{action}</TableCell>
                <TableCell>{jobName}</TableCell>
                <TableCell>{message}</TableCell>
            </TableRow>
        ))}
    </React.Fragment>
)
export default ResultDialog

import React from "react"
import { Dialog, DialogTitle, DialogActions, Button } from "@material-ui/core"
import { useObserver } from "mobx-react"

import useStores from "../hooks/useStores"

interface ConfirmDialogProps {}

const ConfirmDialog: React.FC<ConfirmDialogProps> = () => {
    const { viewStore } = useStores()
    return useObserver(() => {
        const { closeDialog, dialog } = viewStore
        const onYesClick = () => {
            closeDialog()
            dialog.action()
        }
        const onNoClick = () => {
            closeDialog()
        }
        return (
            <Dialog open={dialog.open}>
                <DialogTitle>Are you sure?</DialogTitle>
                <DialogActions>
                    <Button onClick={onYesClick}>yes</Button>
                    <Button onClick={onNoClick}>cancel</Button>
                </DialogActions>
            </Dialog>
        )
    })
}

export default ConfirmDialog

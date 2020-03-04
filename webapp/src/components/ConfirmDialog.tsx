import React from "react"
import { Dialog, DialogTitle, DialogActions, Button } from "@material-ui/core"
import { useObserver } from "mobx-react"

import useStores from "../hooks/useStores"

interface ConfirmDialogProps {}

const ConfirmDialog: React.FC<ConfirmDialogProps> = () => {
    const { viewStore } = useStores()
    const yesButton = React.useRef<HTMLButtonElement | null>(null)

    const onEnter = React.useCallback(() => {
        if (!viewStore.dialog.open) {
            return
        }
        if (!yesButton.current) {
            return
        }
        yesButton.current.focus()
    }, [viewStore.dialog.open])

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
            <Dialog onEnter={onEnter} open={dialog.open} onClose={closeDialog}>
                <DialogTitle>{dialog.label}</DialogTitle>
                <DialogActions>
                    <Button onClick={onYesClick} ref={yesButton}>
                        yes
                    </Button>
                    <Button onClick={onNoClick}>cancel</Button>
                </DialogActions>
            </Dialog>
        )
    })
}

export default ConfirmDialog

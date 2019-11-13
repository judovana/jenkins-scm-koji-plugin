import React from "react"

import { Tooltip, IconButton, Dialog, Button, DialogContent, DialogActions } from "@material-ui/core"
import { Add } from "@material-ui/icons"

import { Item } from "../../stores/model"

interface Props {
    items: Item[]
    label: string
    onAdd: (id: string) => void
}

const AddComponent: React.FC<Props> = ({ items, label, onAdd }) => {

    const [open, setOpen] = React.useState(false)

    const closeDialog = () => {
        setOpen(false)
    }

    const dialog = (
        <Dialog
            open={open}
            disableEscapeKeyDown
            onEscapeKeyDown={closeDialog}
            onBackdropClick={closeDialog}
        >
            <DialogContent dividers>
                {
                    items.map(item => (
                        <Button
                            key={item.id}
                            onClick={() => {
                                onAdd(item.id)
                                closeDialog()
                                }}>
                            {item.id}
                        </Button>
                    ))
                }
            </DialogContent>
            <DialogActions>
                <Button onClick={closeDialog}>cancel</Button>
            </DialogActions>
        </Dialog>
    )

    return (
        <span>
            <Tooltip title={label}>
                <IconButton onClick={() => { setOpen(true) }}>
                    <Add />
                </IconButton>
            </Tooltip>
            {dialog}
        </span>
    )
}

export default AddComponent

import React from "react"

import { Tooltip, IconButton, Dialog, Button, DialogContent, DialogActions } from "@material-ui/core"
import { Add, Check } from "@material-ui/icons"

import { Item } from "../../stores/model"

interface AddItem extends Item {
    marked?: boolean
}

interface Props {
    items: AddItem[]
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
                    items.map(({id, marked}) => (
                        <Button
                            endIcon={marked && <Check />}
                            key={id}
                            onClick={() => {
                                onAdd(id)
                                closeDialog()
                                }}>
                            {id}
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

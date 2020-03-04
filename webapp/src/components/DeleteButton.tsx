import React from "react"
import { IconButton } from "@material-ui/core"
import { Delete } from "@material-ui/icons"

import useStores from "../hooks/useStores"

interface DeleteButtonProps {
    onClick: () => void
}

const DeleteButton: React.FC<DeleteButtonProps> = ({ onClick }) => {
    const { viewStore } = useStores()
    const { confirm } = viewStore
    const _onClick = () => confirm("Are you sure to delete?", onClick)
    return (
        <IconButton onClick={_onClick}>
            <Delete />
        </IconButton>
    )
}

export default DeleteButton

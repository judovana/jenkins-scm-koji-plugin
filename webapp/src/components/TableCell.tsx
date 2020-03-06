import React from "react"
import { TableCell as MUITableCell } from "@material-ui/core"

const TableCell: React.FC = ({ children }) => {
    return <MUITableCell padding="none">{children}</MUITableCell>
}

export default TableCell

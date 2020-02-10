import React from "react"
import { useObserver } from "mobx-react"
import { FormControl, FormLabel, IconButton } from "@material-ui/core"
import { Add, Delete } from "@material-ui/icons"

interface FormListProps {
    data: any[]
    label: string
    renderItem: (item: any) => React.ReactNode
}

const FormList: React.FC<FormListProps> = props => {
    return useObserver(() => {
        const { data, label, renderItem } = props
        return (
            <div>
                <FormControl fullWidth margin="normal">
                    <FormLabel>{label}</FormLabel>
                    {data.map((item, index) => (
                        <div style={{ flexDirection: "row" }} key={index}>
                            {renderItem(item)}
                            <IconButton onClick={() => data.splice(index, 1)}>
                                <Delete />
                            </IconButton>
                        </div>
                    ))}
                </FormControl>
                <IconButton onClick={() => data.push({})}>
                    <Add />
                </IconButton>
            </div>
        )
    })
}

export default FormList

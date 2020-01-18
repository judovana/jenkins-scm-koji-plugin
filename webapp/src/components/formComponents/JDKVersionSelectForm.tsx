import React from "react"
import { FormControl, FormLabel, FormGroup } from "@material-ui/core"
import { useObserver } from "mobx-react"

import Select from "./Select"
import { Product } from "../../stores/model"
import useStores from "../../hooks/useStores"

interface Props {
    product: Product
}

const ProductSelectForm: React.FC<Props> = props => {

    const { configStore } = useStores()

    return useObserver(() => {
        const { product } = props
        const { jdkVersions, getJDKVersion } = configStore
        const jdkVersion = getJDKVersion(product.jdk)

        const onJDKVersionChange = (value: string) => {
            product.jdk = value
        }

        const onPackageNameChange = (value: string) => {
            product.packageName = value
        }

        return (
            <FormControl fullWidth margin="normal">
                <FormLabel>
                    Product
                </FormLabel>
                <FormGroup>
                    <Select
                        label="JDK version"
                        onChange={onJDKVersionChange}
                        options={jdkVersions.map(v => v.id)}
                        value={product.jdk} />
                    {
                        jdkVersion && <Select
                            label="package name"
                            onChange={onPackageNameChange}
                            options={jdkVersion.packageNames}
                            value={
                                jdkVersion.packageNames.length >= 0 ?
                                    jdkVersion.packageNames[0] : ""
                            } />
                    }
                </FormGroup>
            </FormControl>
        )
    })
}

export default ProductSelectForm
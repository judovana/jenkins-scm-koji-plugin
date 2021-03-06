import React from "react"
import { FormControl, FormLabel, FormGroup } from "@material-ui/core"
import { useObserver } from "mobx-react"

import Select from "./Select"
import { Product } from "../../stores/model"
import useStores from "../../hooks/useStores"
import { ProductValidation } from "../../utils/validators"

interface Props {
    product: Product
    validation?: ProductValidation
}

const ProductSelectForm: React.FC<Props> = props => {
    const { configStore } = useStores()

    return useObserver(() => {
        const { product, validation } = props
        const { jdkVersions, getJDKVersion } = configStore
        const jdkVersion = getJDKVersion(product.jdk)

        const {
            jdk,
            packageName
        } = validation || {} as ProductValidation

        const onJDKVersionChange = (value: string) => {
            product.jdk = value
            const jdkVersion = getJDKVersion(product.jdk)
            if (jdkVersion) {
                const packageNames = jdkVersion.packageNames
                product.packageName =
                    packageNames.length > 0 ? packageNames[0] : ""
            }
        }

        const onPackageNameChange = (value: string) => {
            product.packageName = value
        }

        return (
            <FormControl fullWidth margin="normal">
                <FormLabel>Product</FormLabel>
                <FormGroup>
                    <Select
                        label="JDK platform version"
                        onChange={onJDKVersionChange}
                        options={jdkVersions.map(v => v.id)}
                        validation={jdk}
                        value={product.jdk}
                    />
                    {jdkVersion && (
                        <Select
                            label="package name"
                            onChange={onPackageNameChange}
                            options={jdkVersion.packageNames}
                            validation={packageName}
                            value={product.packageName}
                        />
                    )}
                </FormGroup>
            </FormControl>
        )
    })
}

export default ProductSelectForm

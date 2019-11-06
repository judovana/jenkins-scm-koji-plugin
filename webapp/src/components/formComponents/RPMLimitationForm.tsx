import React from "react"
import { FormControl, FormLabel, FormGroup } from "@material-ui/core"

import Select from "./Select"
import { RPMLimitation } from "../../stores/model"
import TextInput from "./TextInput"

interface Props {
    rpmLimitation: RPMLimitation
    onFlagChange: (value: string) => void
    onGlobChange: (value: string) => void
}

const RPMLimitationForm: React.FunctionComponent<Props> = (props) => {

    const { rpmLimitation, onFlagChange, onGlobChange } = props

    return (
        <FormControl margin="normal">
            <FormLabel>
                rpm limitation
            </FormLabel>
            <FormGroup>
                <Select
                    onChange={onFlagChange}
                    options={["WHITELIST", "BLACKLIST"]}
                    value={rpmLimitation.flag} />
                {
                    rpmLimitation.flag !== "" &&
                    <TextInput
                        label={"glob"}
                        onChange={onGlobChange}
                        placeholder={"Enter glob"}
                        value={rpmLimitation.glob} />
                }
            </FormGroup>
        </FormControl>
    )
}

export default RPMLimitationForm

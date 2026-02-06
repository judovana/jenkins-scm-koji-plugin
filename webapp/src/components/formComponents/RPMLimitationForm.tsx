import React from "react"
import { Grid } from "@material-ui/core"
import { useObserver } from "mobx-react"

import { RPMLimitation } from "../../stores/model"
import TextInput from "./TextInput"

interface Props {
    rpmLimitation: RPMLimitation
}

const RPMLimitationForm: React.FunctionComponent<Props> = ({ rpmLimitation }) => {

    const onRPMLimitationDenylistChange = (value: string) => {
        rpmLimitation.denylist = value.split(" ")
    }

    const onRPMLimitationAllowlistChange = (value: string) => {
        rpmLimitation.allowlist = value.split(" ")
    }

    return useObserver(() => {
        return (
            <React.Fragment>
                <Grid container item xs={12}>
                    <TextInput
                        label={"subpackage denylist"}
                        onChange={onRPMLimitationDenylistChange}
                        value={rpmLimitation.denylist.join(" ")} />
                    <TextInput
                        label={"subpackage allowlist"}
                        onChange={onRPMLimitationAllowlistChange}
                        value={rpmLimitation.allowlist.join(" ")} />
                </Grid>

            </React.Fragment>
        )
    })
}

export default RPMLimitationForm

import React from "react"
import { Grid } from "@material-ui/core"
import { useObserver } from "mobx-react"

import { RPMLimitation } from "../../stores/model"
import TextInput from "./TextInput"

interface Props {
    rpmLimitation: RPMLimitation
}

const RPMLimitationForm: React.FunctionComponent<Props> = ({ rpmLimitation }) => {

    const onRPMLimitationBlacklistChange = (value: string) => {
        rpmLimitation.blacklist = value.split(" ")
    }

    const onRPMLimitationWhitelistChange = (value: string) => {
        rpmLimitation.whitelist = value.split(" ")
    }

    return useObserver(() => {
        return (
            <React.Fragment>
                <Grid container item xs={12}>
                    <TextInput
                        label={"subpackage blacklist"}
                        onChange={onRPMLimitationBlacklistChange}
                        value={rpmLimitation.blacklist.join(" ")} />
                    <TextInput
                        label={"subpackage whitelist"}
                        onChange={onRPMLimitationWhitelistChange}
                        value={rpmLimitation.whitelist.join(" ")} />
                </Grid>

            </React.Fragment>
        )
    })
}

export default RPMLimitationForm

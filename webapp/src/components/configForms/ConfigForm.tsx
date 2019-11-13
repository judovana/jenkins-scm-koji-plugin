import React from "react";
import { inject, observer } from "mobx-react"
import { Button, Snackbar } from "@material-ui/core"

import { ConfigStore, CONFIG_STORE } from "../../stores/ConfigStore"
import { JDKProject, Task, Platform, Item, ConfigState } from "../../stores/model"
import JDKProjectForm from "./JDKProjectForm";
import TaskForm from "./TaskForm";
import PlatformForm from "./PlatformForm";

interface Props {
    configStore?: ConfigStore
}

interface SnackbarState {
    open: boolean
    message?: string
    actions?: JSX.Element[]
}

const ConfigForm: React.FC<Props> = props => {

    const { createConfig, configError, jobUpdateResults, updateConfig, discardOToolResponse } = props.configStore!

    const okButton = (
        <Button
            color="secondary"
            key="ok"
            onClick={() => { discardOToolResponse() }}
            size="small">
            OK
        </Button>
    )

    const snackbarState: SnackbarState | undefined = (configError && {
        open: true,
        message: configError,
        actions: [
            okButton
        ]
    }) || (jobUpdateResults && {
        open: true,
        message: "Done, see console output",
        actions: [
            okButton
        ]
    })

    const onSubmit = async (config: Item, state: ConfigState) => {
        let updateFunction: (item: Item) => void
        switch (state) {
            case "create":
                updateFunction = createConfig
                break
            case "update":
                updateFunction = updateConfig
                break
            default:
                return
        }
        updateFunction(config)
    }

    const renderForm = () => {
        const { selectedConfig, selectedGroupId } = props.configStore!;
        if (!selectedConfig) {
            return null
        }
        switch (selectedGroupId) {
            case "jdkProjects":
                return (
                    <JDKProjectForm
                        onSubmit={onSubmit}
                        project={selectedConfig as JDKProject} />
                );
            case "tasks":
                return (
                    <TaskForm
                        onSubmit={onSubmit}
                        task={selectedConfig as Task} />
                );
            case "platforms":
                return (
                    <PlatformForm
                        onSubmit={onSubmit}
                        platform={selectedConfig as Platform} />
                )
            default:
                return null;
        }
    }

    return (
        <div>
            <div className="form-container">
                <form>
                    {renderForm()}
                </form>
            </div>
            {
                snackbarState && <Snackbar
                    action={snackbarState.actions}
                    anchorOrigin={{
                        horizontal: "center",
                        vertical: "top"
                    }}
                    autoHideDuration={10000}
                    message={<span>{(snackbarState.message || "").toString()}</span>}
                    open={snackbarState.open} />
            }
        </div>
    )
}

export default inject(CONFIG_STORE)(observer(ConfigForm));

import React from "react"
import { useObserver } from "mobx-react"
import { Button, Snackbar, Paper } from "@material-ui/core"

import { Platform, JDKTestProject, JDKProject, Task } from "../../stores/model"
import JDKProjectForm from "./JDKProjectForm"
import TaskForm from "./TaskForm"
import PlatformForm from "./PlatformForm"
import JDKTestProjectForm from "./JDKTestProjectForm"
import useStores from "../../hooks/useStores"
import {
    PlatformValidation,
    JDKProjectValidation,
    JDKTestProjectValidation,
    TaskValidation
} from "../../utils/validators"

interface SnackbarState {
    open: boolean
    message?: string
    actions?: JSX.Element[]
}

const ConfigForm: React.FC = () => {
    const { configStore, viewStore } = useStores()

    return useObserver(() => {
        const {
            configState,
            editedConfig,
            selectedConfigGroupId,
            configValidation
        } = configStore
        if (!selectedConfigGroupId) {
            return <div>{"ooops"}</div>
        }
        const {
            configError,
            discardOToolResponse,
            submit
        } = configStore
        const okButton = (
            <Button
                color="secondary"
                key="ok"
                onClick={() => {
                    discardOToolResponse()
                }}
                size="small">
                OK
            </Button>
        )

        const snackbarState: SnackbarState | undefined =
            (configError && {
                open: true,
                message: configError,
                actions: [okButton]
            }) || undefined

        const renderForm = () => {
            switch (selectedConfigGroupId) {
                case "jdkProjects":
                    return (
                        <JDKProjectForm
                            config={editedConfig as JDKProject}
                            validation={
                                configValidation as JDKProjectValidation
                            }
                        />
                    )
                case "jdkTestProjects":
                    return (
                        <JDKTestProjectForm
                            config={editedConfig as JDKTestProject}
                            validation={
                                configValidation as JDKTestProjectValidation
                            }
                        />
                    )
                case "tasks":
                    return (
                        <TaskForm
                            config={editedConfig as Task}
                            validation={configValidation as TaskValidation}
                        />
                    )
                case "platforms":
                    return (
                        <PlatformForm
                            config={editedConfig as Platform}
                            validation={configValidation as PlatformValidation}
                        />
                    )
                default:
                    return null
            }
        }

        return (
            <React.Fragment>
                <Paper style={{ padding: 20, width: "100%" }}>
                    {renderForm()}
                    <Button
                        disabled={configState === "pending"}
                        onClick={() => viewStore.confirm("Are you sure?", submit)}
                        variant="contained">
                        {(configState === "edit" && "Edit") ||
                            (configState === "new" && "Create") ||
                            (configState === "pending" && "...")}
                    </Button>
                </Paper>
                {snackbarState && (
                    <Snackbar
                        action={snackbarState.actions}
                        anchorOrigin={{
                            horizontal: "center",
                            vertical: "top"
                        }}
                        autoHideDuration={10000}
                        message={
                            <span>
                                {(snackbarState.message || "").toString()}
                            </span>
                        }
                        open={snackbarState.open}
                    />
                )}
            </React.Fragment>
        )
    })
}

export default ConfigForm

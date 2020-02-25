import React from "react";
import { useObserver, useLocalStore } from "mobx-react"
import { Task, TaskType, MachinePreference, Item } from "../../stores/model"
import LimitationForm from "../formComponents/LimitationForm"
import TextInput from "../formComponents/TextInput"
import TextArea from "../formComponents/TextArea"
import Select from "../formComponents/Select"
import RPMLimitationForm from "../formComponents/RPMLimitationForm"
import FileRequirementsForm from "../formComponents/FileRequirementsForm"
import { Button } from "@material-ui/core"
import useStores from "../../hooks/useStores"
import FormList from "../formComponents/FormList"
import VariableForm from "../formComponents/VariableForm"

type TaskFormProps = {
    taskID?: string,
    onSubmit: (item: Item) => void
}

const TaskForm: React.FC<TaskFormProps> = props => {

    const { configStore } = useStores()

    const { taskID } = props

    const task = useLocalStore<Task>(() => ({
        fileRequirements: {
            binary: "NONE",
            source: false,
            noarch: false
        },
        id: "",
        machinePreference: "VM",
        platformLimitation: {
            flag: "NONE",
            list: []
        },
        productLimitation: {
            flag: "NONE",
            list: []
        },
        rpmLimitation: {
            blacklist: [],
            whitelist: []
        },
        scmPollSchedule: "",
        script: "",
        type: "TEST",
        xmlTemplate: "",
        variables: []
    }))

    React.useEffect(() => {
        if (taskID === undefined || taskID === task.id) {
            return
        }
        const _task = configStore.getTask(taskID)
        if (!_task) {
            return
        }
        task.fileRequirements = _task.fileRequirements
        task.id = _task.id
        task.machinePreference = _task.machinePreference
        task.platformLimitation = _task.platformLimitation
        task.productLimitation = _task.productLimitation
        task.rpmLimitation = _task.rpmLimitation
        task.scmPollSchedule = _task.scmPollSchedule
        task.script = _task.script
        task.type = _task.type
        task.xmlTemplate = _task.xmlTemplate
        task.variables = _task.variables || []
    })

    const onIdChange = (value: string) => {
        task.id = value
    }

    const onTypeChange = (value: string) => {
        task.type = value as TaskType
    }

    const onSCMPollScheduleChange = (value: string) => {
        task.scmPollSchedule = value
    }

    const onMachinePreferenceChange = (value: string) => {
        task.machinePreference = value as MachinePreference
    }

    const onScriptChange = (value: string) => {
        task.script = value
    }

    const onXmlTemplateChange = (value: string) => {
        task.xmlTemplate = value
    }

    const onSubmit = () => {
        const { rpmLimitation } = task
        rpmLimitation.blacklist = rpmLimitation.blacklist.filter(item => item)
        rpmLimitation.whitelist = rpmLimitation.whitelist.filter(item => item)
        props.onSubmit(task)
    }

    return useObserver(() => {
        const {
            id,
            fileRequirements,
            platformLimitation,
            productLimitation,
            rpmLimitation,
            variables
        } = task

        return (
            <React.Fragment>
                <TextInput
                    label={"Task id"}
                    onChange={onIdChange}
                    value={id} />
                <Select
                    label={"type"}
                    onChange={onTypeChange}
                    options={["BUILD", "TEST"]}
                    value={task.type} />
                <Select
                    label={"machine preference"}
                    onChange={onMachinePreferenceChange}
                    options={["VM", "VM_ONLY", "HW", "HW_ONLY"]}
                    value={task.machinePreference} />
                <TextInput
                    label={"SCM poll schedule"}
                    onChange={onSCMPollScheduleChange}
                    value={task.scmPollSchedule}>
                </TextInput>
                <TextInput
                    label={"script"}
                    value={task.script}
                    onChange={onScriptChange}
                    placeholder={"Enter path to bash script"} />
                <LimitationForm
                    label={"platform limitations"}
                    limitation={platformLimitation}
                    items={configStore.platforms} />
                <LimitationForm
                    label={"product limitations"}
                    limitation={productLimitation}
                    items={configStore.jdkVersions} />
                <FileRequirementsForm
                    fileRequirements={fileRequirements} />
                <TextArea
                    label={"xml template"}
                    onChange={onXmlTemplateChange}
                    placeholder={"Enter xml template for post build tasks"}
                    value={task.xmlTemplate} />
                <RPMLimitationForm
                    rpmLimitation={rpmLimitation} />
                <FormList
                    data={variables}
                    label="custom variables"
                    renderItem={item => <VariableForm variable={item} />}/>
                <Button
                    color="primary"
                    onClick={onSubmit}
                    variant="contained">
                    {taskID === undefined ? "Create" : "Update"}
                </Button>
            </React.Fragment>
        )
    })
}

export default TaskForm

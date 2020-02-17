import React from "react"
import { useObserver } from "mobx-react"
import { Task, TaskType, MachinePreference } from "../../stores/model"
import LimitationForm from "../formComponents/LimitationForm"
import TextInput from "../formComponents/TextInput"
import TextArea from "../formComponents/TextArea"
import Select from "../formComponents/Select"
import RPMLimitationForm from "../formComponents/RPMLimitationForm"
import FileRequirementsForm from "../formComponents/FileRequirementsForm"
import useStores from "../../hooks/useStores"
import FormList from "../formComponents/FormList"
import VariableForm from "../formComponents/VariableForm"

type TaskFormProps = {
    config: Task
}

const TaskForm: React.FC<TaskFormProps> = props => {
    const { configStore } = useStores()

    return useObserver(() => {
        const { config: task } = props

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
                <TextInput label={"Task id"} onChange={onIdChange} value={id} />
                <Select
                    label={"type"}
                    onChange={onTypeChange}
                    options={["BUILD", "TEST"]}
                    value={task.type}
                />
                <Select
                    label={"machine preference"}
                    onChange={onMachinePreferenceChange}
                    options={["VM", "VM_ONLY", "HW", "HW_ONLY"]}
                    value={task.machinePreference}
                />
                <TextInput
                    label={"SCM poll schedule"}
                    onChange={onSCMPollScheduleChange}
                    value={task.scmPollSchedule}></TextInput>
                <TextInput
                    label={"script"}
                    value={task.script}
                    onChange={onScriptChange}
                    placeholder={"Enter path to bash script"}
                />
                <LimitationForm
                    label={"platform limitations"}
                    limitation={platformLimitation}
                    items={configStore.platforms}
                />
                <LimitationForm
                    label={"product limitations"}
                    limitation={productLimitation}
                    items={configStore.jdkVersions}
                />
                <FileRequirementsForm fileRequirements={fileRequirements} />
                <TextArea
                    label={"xml template"}
                    onChange={onXmlTemplateChange}
                    placeholder={"Enter xml template for post build tasks"}
                    value={task.xmlTemplate}
                />
                <RPMLimitationForm rpmLimitation={rpmLimitation} />
                <FormList
                    data={variables}
                    label="custom variables"
                    renderItem={item => <VariableForm variable={item} />}
                />
            </React.Fragment>
        )
    })
}

export default TaskForm

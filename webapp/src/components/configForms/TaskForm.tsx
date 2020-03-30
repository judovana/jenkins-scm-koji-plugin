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
import { createDefaultVariable } from "../../utils/defaultConfigs"
import {
    TaskValidation,
    setDefaultValidations,
    VariableValidation
} from "../../utils/validators"

type TaskFormProps = {
    config: Task
    validation?: TaskValidation
}

const TaskForm: React.FC<TaskFormProps> = props => {
    const { configStore } = useStores()

    return useObserver(() => {
        const { config: task, validation } = props

        const onTimeoutInHours = (value: string) => {
            task.timeoutInHours = value
        }

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

        const onXmlViewTemplateChange = (value: string) => {
            task.xmlViewTemplate = value
        }

        const {
            id,
            fileRequirements,
            platformLimitation,
            productLimitation,
            rpmLimitation,
            timeoutInHours,
            variables
        } = task

        const { id: idValidation, script: scriptValidation, timeoutInHours: timeoutInHoursValidation } =
            validation || ({} as TaskValidation)

        const variablesValidation = setDefaultValidations<VariableValidation>(
            validation && validation.variables,
            variables
        )

        return (
            <React.Fragment>
                <TextInput
                    label={"Task id"}
                    onChange={onIdChange}
                    validation={idValidation}
                    value={id}
                />
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
                    validation={scriptValidation}
                    value={task.script}
                    onChange={onScriptChange}
                />
                 <TextInput
                    label={"timeoutInHours"}
                    validation={timeoutInHoursValidation}
                    value={task.timeoutInHours.toString()}
                    onChange={onTimeoutInHours}
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
                <TextArea
                    label={"xml VIEW columns template"}
                    onChange={onXmlViewTemplateChange}
                    placeholder={"Enter xml view columns template for post build tasks"}
                    value={task.xmlViewTemplate}
                />
                <RPMLimitationForm rpmLimitation={rpmLimitation} />
                <FormList
                    data={variables}
                    label="custom variables"
                    onAdd={createDefaultVariable}
                    renderItem={(item, index) => (
                        <VariableForm
                            variable={item}
                            validation={variablesValidation[index]}
                        />
                    )}
                />
            </React.Fragment>
        )
    })
}

export default TaskForm

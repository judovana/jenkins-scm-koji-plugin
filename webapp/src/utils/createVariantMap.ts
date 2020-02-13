import { TaskVariant } from "../stores/model"

const createTaskVariantsMap = (
    taskVariants: TaskVariant[],
    filter: (variant: TaskVariant) => boolean
): { [key: string]: string } =>
    taskVariants.filter(filter).reduce((map, taskVariant) => {
        map[taskVariant.id] = taskVariant.defaultValue
        return map
    }, {} as { [key: string]: string })

export default createTaskVariantsMap

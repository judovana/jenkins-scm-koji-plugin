import {
    BuildConfigs,
    TaskVariant,
    PlatformConfig,
    TaskConfig,
    VariantsConfig,
} from "../stores/model"

type JenkinsJob = {
    name: string
    url: string
}

export const getJobNameGenerator = (
    variants: { [id: string]: TaskVariant },
    url: string | undefined,
): ((
    projectId: string,
    jdkId: string,
    platformConfig: PlatformConfig,
    taskConfig: TaskConfig | undefined,
    variantsConfig: VariantsConfig,
    buildConfigs: BuildConfigs | undefined,
) => JenkinsJob | null) => {
    if (!url) {
        return () => null
    }
    const variantsMap = Object.keys(variants).reduce((map, key) => {
        const variant = variants[key]
        map.set(key, variant)
        return map
    }, new Map<string, TaskVariant>())
    const sortVariants = (a: string, b: string): number => {
        const variantA = variantsMap.get(a)
        const variantB = variantsMap.get(b)
        if (!variantA || !variantB) {
            return 0
        }
        return variantA.order - variantB.order
    }
    const variantsToString = (config: VariantsConfig): string => {
        return Object.keys(config.map)
            .sort(sortVariants)
            .map(key => config.map[key])
            .join(".")
    }
    return (projectId, jdkId, platform, task, variants, buildConfigs) => {
        if (!task) {
            return null
        }
        const buildPart = !!buildConfigs
            ? `-${buildConfigs.platform.id}-${variantsToString(buildConfigs.taskVariants)}-`
            : "-"
        const name = `${task.id}-${jdkId}-${projectId}${buildPart}${platform.id}.${platform.provider}-${variantsToString(variants)}`
        const jobUrl = `${url}job/${name}`
        return {
            name,
            url: jobUrl,
        }
    }
}

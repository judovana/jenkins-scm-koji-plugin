import {
    Platform,
    PlatformProvider,
    Variable,
    ConfigGroupId,
    Task,
    JDKProject,
    JDKTestProject,
    JDKVersion,
    BuildProvider,
    Product,
    TaskVariant
} from "../stores/model"

type StringInputValue = string | null | undefined
type StringListInputValue = string[] | null | undefined
type CheckboxValue = boolean | null | undefined

const isNumeric = (value: string) =>
    !isNaN(parseFloat(value)) && isFinite(value as any)

const requiredStringValidator = (value: StringInputValue): BasicValidation => {
    if (!value || !value.trim()) {
        return "required"
    }
    return "ok"
}

const stringListValidator = (required: boolean) => (
    value: StringListInputValue
): BasicValidation => {
    if (required && (!value || value.length === 0)) {
        return "required"
    }
    if (value && value.some(val => !val)) {
        return "whitespaces"
    }
    return "ok"
}

const requiredStringListValidator = stringListValidator(true)
const optionalStringListValidator = stringListValidator(false)

const numericStringValidator = (value: string): BasicValidation => {
    const firstValidation = requiredStringValidator(value)
    if (firstValidation !== "ok") {
        return firstValidation
    }
    return isNumeric(value) ? "ok" : "invalid"
}

const optionalValidator = (
    _: StringInputValue | CheckboxValue
): BasicValidation => "ok"

const productValidator = ({ jdk, packageName }: Product) => ({
    jdk: requiredStringValidator(jdk),
    packageName: requiredStringValidator(packageName)
})

const variablesValidator = (variables: Variable[]): VariableValidation[] =>
    variables.map(variableValidator)

const variableValidator = ({
    comment,
    commentedOut,
    defaultPrefix,
    exported,
    name,
    value
}: Variable) => ({
    comment: optionalValidator(comment),
    commentedOut: optionalValidator(commentedOut),
    defaultPrefix: optionalValidator(defaultPrefix),
    exported: optionalValidator(exported),
    name: requiredStringValidator(name),
    value: requiredStringValidator(value)
})

const platformProvidersValidator = (
    providers: PlatformProvider[]
): PlatformProviderValidation[] => providers.map(platformProviderValidator)

const platformProviderValidator = ({
    hwNodes,
    id,
    vmNodes
}: PlatformProvider) => ({
    hwNodes: optionalStringListValidator(hwNodes),
    id: requiredStringValidator(id),
    vmNodes: optionalStringListValidator(vmNodes)
})

const buildProviderValidator = ({ id }: BuildProvider) => ({
    id: requiredStringValidator(id)
})

const jdkProjectValidator = ({ id, product, url }: JDKProject) => ({
    buildProviders: "ok",
    id: requiredStringValidator(id),
    jobConfiguration: "ok",
    product: productValidator(product),
    repoState: "ok",
    type: "ok",
    url: requiredStringValidator(url)
})

const jdkTestProjectValidator = ({
    id,
    product,
    subpackageBlacklist,
    subpackageWhitelist
}: JDKTestProject) => ({
    buildProviders: "ok",
    id: requiredStringValidator(id),
    jobConfiguration: "ok",
    product: productValidator(product),
    subpackageBlacklist: optionalStringListValidator(subpackageBlacklist),
    subpackageWhitelist: optionalStringListValidator(subpackageWhitelist),
    type: "ok"
})

const jdkVersionValidator = (_: JDKVersion) => ({
    id: "ok",
    packageNames: optionalStringListValidator
})

const taskVariantValidator = (_: TaskVariant) => ({
    defaultValue: "ok",
    id: "ok",
    supportsSubpackages: "ok",
    type: "ok",
    variants: "ok"
})

const platformValidator = ({
    architecture,
    id,
    kojiArch,
    os,
    providers,
    tags,
    variables,
    version,
    versionNumber,
    vmName
}: Platform) => ({
    architecture: requiredStringValidator(architecture),
    id: optionalValidator(id),
    kojiArch: optionalValidator(kojiArch),
    os: requiredStringValidator(os),
    providers: platformProvidersValidator(providers),
    tags: requiredStringListValidator(tags),
    variables: variablesValidator(variables),
    version: requiredStringValidator(version),
    versionNumber: numericStringValidator(versionNumber),
    vmName: requiredStringValidator(vmName)
})

const taskValidator = ({ id, script, variables }: Task) => ({
    fileRequirements: "ok",
    id: requiredStringValidator(id),
    machinePreference: "ok",
    platformLimitation: "ok",
    productLimitation: "ok",
    rpmLimitation: "ok",
    scmPollSchedule: "ok",
    script: requiredStringValidator(script),
    type: "ok",
    variables: variablesValidator(variables),
    xmlTemplate: "ok"
})

export const validators: { [id in ConfigGroupId]: ConfigValidator } = {
    buildProviders: buildProviderValidator,
    jdkProjects: jdkProjectValidator,
    jdkTestProjects: jdkTestProjectValidator,
    jdkVersions: jdkVersionValidator,
    platforms: platformValidator,
    taskVariants: taskVariantValidator,
    tasks: taskValidator
}

export const checkValidation = (validation?: Validation): boolean => {
    if (!validation) {
        return false
    }
    if (typeof validation === "object") {
        return Object.values(validation).reduce(
            (acc: boolean, curr: Validation | undefined) => {
                return acc && checkValidation(curr)
            },
            true
        )
    } else if (typeof validation === "string") {
        return validation === "ok"
    } else {
        return false
    }
}

export const isConfigValid = (validation: ConfigValidation): boolean =>
    checkValidation(validation)

export const setDefaultValidations = <T>(
    validations: T[] | undefined,
    formFields: any[]
) =>
    validations && validations.length === formFields.length
        ? validations
        : formFields.map(_ => undefined)

export type PlatformValidation = ReturnType<typeof platformValidator>
export type BuildProviderValidator = typeof buildProviderValidator
export type JDKTestProjectValidator = typeof jdkTestProjectValidator
export type JDKVersionValidator = typeof jdkVersionValidator
export type JDKProjectValidator = typeof jdkProjectValidator
export type TaskVariantValidator = typeof taskVariantValidator
export type PlatformValidator = typeof platformValidator
export type TaskValidator = typeof taskValidator

export type ConfigValidation =
    | BuildProviderValidation
    | JDKVersionValidation
    | JDKProjectValidation
    | JDKTestProjectValidation
    | PlatformValidation
    | TaskValidation
    | TaskVariantValidation

export type ConfigValidator =
    | BuildProviderValidator
    | JDKVersionValidator
    | JDKProjectValidator
    | JDKTestProjectValidator
    | PlatformValidator
    | TaskValidator
    | TaskVariantValidator

export type ProductValidation = ReturnType<typeof productValidator>
export type BuildProviderValidation = ReturnType<typeof variableValidator>
export type JDKProjectValidation = ReturnType<typeof jdkProjectValidator>
export type JDKTestProjectValidation = ReturnType<
    typeof jdkTestProjectValidator
>
export type JDKVersionValidation = ReturnType<typeof jdkVersionValidator>
export type TaskValidation = ReturnType<typeof taskValidator>
export type TaskVariantValidation = ReturnType<typeof taskVariantValidator>
export type VariableValidation = ReturnType<typeof variableValidator>
export type PlatformProviderValidation = ReturnType<
    typeof platformProviderValidator
>

export type BasicValidation = "invalid" | "required" | "ok" | "whitespaces"

export type Validation =
    | BasicValidation
    | ConfigValidation
    | VariableValidation[]
    | VariableValidation

package org.fakekoji.core.utils.matrix;

import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.core.utils.matrix.cell.CellGroup;
import org.fakekoji.core.utils.matrix.cell.Cell;
import org.fakekoji.core.utils.matrix.cell.MultiUrlCell;
import org.fakekoji.core.utils.matrix.cell.TitleCell;
import org.fakekoji.core.utils.matrix.cell.UpperCornerCell;
import org.fakekoji.core.utils.matrix.cell.UrlCell;
import org.fakekoji.core.utils.matrix.formatter.Formatter;
import org.fakekoji.core.utils.matrix.formatter.PlainTextFormatter;
import org.fakekoji.jobmanager.ConfigCache;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.model.BuildJob;
import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.jobmanager.model.Product;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.jobmanager.model.TaskJob;
import org.fakekoji.jobmanager.model.TestJob;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantValue;
import org.fakekoji.storage.StorageException;
import org.fakekoji.xmlrpc.server.JavaServerConstants;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MatrixGenerator {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

    private static final TestEqualityFilter defaultTestFilter = new TestEqualityFilter(true, true, true, true, true);
    private static final BuildEqualityFilter defaultBuildFilter = new BuildEqualityFilter(true, true, true, true, true, true);
    private static final String defaultRegex = ".*";

    private final TestEqualityFilter testFilter;
    private final BuildEqualityFilter buildFilter;
    private final Pattern testRegex;
    private final Pattern buildRgex;
    private final String[] project;
    private final ConfigCache cache;
    private final AccessibleSettings settings;

    private final static String EXTERNAL_PLATFORM_PROVIDER = "external";


    public MatrixGenerator(final AccessibleSettings settings, String[] project) {
        this(settings, defaultRegex, defaultRegex, defaultTestFilter, defaultBuildFilter, project);

    }

    public MatrixGenerator(final AccessibleSettings settings, String testRegex, String buildRegex, String[] project) {
        this(settings, testRegex, buildRegex, defaultTestFilter, defaultBuildFilter, project);

    }

    public MatrixGenerator(
            final AccessibleSettings settings,
            String testRegex,
            String buildRegex,
            TestEqualityFilter testEqualityFilter,
            BuildEqualityFilter buildEqualityFilter,
            String[] project
    ) {
        this.settings = settings;
        this.testFilter = testEqualityFilter;
        this.buildFilter = buildEqualityFilter;
        this.testRegex = Pattern.compile(testRegex == null ? defaultRegex : testRegex);
        this.buildRgex = Pattern.compile(buildRegex == null ? defaultRegex : buildRegex);

        this.project = project;
        if (project != null) {
            Arrays.sort(project);
        }

        try {
            cache = new ConfigCache(settings.getConfigManager());
        } catch (StorageException se) {
            throw new RuntimeException(se);
        }

    }

    private List<List<String>> getTaskVariantValuesProduct(Collection<TaskVariant> taskVariants) {
        return cartesianProduct(
                taskVariants
                        .stream()
                        .map(variant -> variant.getVariants()
                                .values()
                                .stream()
                                .map(TaskVariantValue::getId)
                                .collect(Collectors.toList())
                        )
                        .collect(Collectors.toList())
        );
    }

    public List<TestSpec> getTests() {
        final Collection<Platform.Provider> providers = Arrays.asList(createNoneProvider(), createExternalProvider());
        final Map<String, TestSpec> map = new LinkedHashMap<>();
        final List<List<String>> variantsProducts = getTaskVariantValuesProduct(cache.getTestTaskVariants());
        for (Platform origPlatform : cache.getPlatforms()) {
            Platform platform = clonePlatformForProviders(origPlatform);
            final Collection<Platform.Provider> platformProviders = Stream.concat(
                    providers.stream(),
                    platform.getProviders().stream()
            ).collect(Collectors.toList());
            for (Platform.Provider provider : platformProviders) {
                for (Task task : cache.getTasks()) {
                    if (!task.getType().equals(Task.Type.TEST)) {
                        TestSpec t = new TestSpec(platform, provider, task, testFilter);
                        if (testRegex.matcher(t.toString()).matches()) {
                            map.put(t.toString(), t); //r.add(t);
                        }
                    } else {
                        for (List<String> variantsProduct : variantsProducts) {
                            TestSpec t = new TestSpec(platform, provider, task, variantsProduct, testFilter);
                            if (testRegex.matcher(t.toString()).matches()) {
                                map.put(t.toString(), t); //r.add(t);
                            }
                        }
                    }
                }
            }
        }
        return new ArrayList<>(map.values());
    }

    private Platform clonePlatformForProviders(Platform origPlatform) {
        if (origPlatform.getProviders().isEmpty()) {
            return new Platform(
                    origPlatform.getId(),
                    origPlatform.getOs(),
                    origPlatform.getVersion(),
                    origPlatform.getVersionNumber(),
                    origPlatform.getArchitecture(),
                    origPlatform.getKojiArch().orElse(null),
                    createNoneProviderList(),
                    origPlatform.getVmName(),
                    origPlatform.getTestingYstream(),
                    origPlatform.getStableZstream(),
                    origPlatform.getTags(),
                    origPlatform.getVariables());
        } else {
            return origPlatform;
        }
    }

    private List<Platform.Provider> createNoneProviderList() {
        return Arrays.asList(createNoneProvider());
    }

    private Platform.Provider createNoneProvider() {
        return new Platform.Provider(
                "noProvider",
                Collections.emptyList(),
                Collections.emptyList());
    }

    private Platform.Provider createExternalProvider() {
        return new Platform.Provider(
                EXTERNAL_PLATFORM_PROVIDER,
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    public List<BuildSpec> getBuilds() {
        final Collection<Platform.Provider> providers = Arrays.asList(createNoneProvider(), createExternalProvider());
        final Collection<TaskVariant> allVariants = cache.getBuildTaskVariants();
        final Collection<TaskVariant> testOnlyVariants = allVariants
                .stream()
                .filter(TaskVariant::isSupportsSubpackages)
                .collect(Collectors.toList());
        final List<List<String>> variantsProducts = Stream.of(
                getTaskVariantValuesProduct(allVariants).stream(),
                getTaskVariantValuesProduct(testOnlyVariants).stream()
        )
                .flatMap(s -> s)
                .collect(Collectors.toList());
        final Map<String, BuildSpec> map = new LinkedHashMap<>();
        for (Platform origPlatform : cache.getPlatforms()) {
            Platform platform = clonePlatformForProviders(origPlatform);
            final Collection<Platform.Provider> platformProviders = Stream.concat(
                    providers.stream(),
                    platform.getProviders().stream()
            ).collect(Collectors.toList());
            for (Platform.Provider provider : platformProviders) {
                for (Project project : cache.getProjects())
                    if (matchProject(project.getId())) {
                        for (List<String> variantProduct : variantsProducts) {
                            BuildSpec b = new BuildSpec(platform, provider, project, variantProduct, buildFilter);
                            if (buildRgex.matcher(b.toString()).matches()) {
                                map.put(b.toString(), b); //r.add(b);
                            }
                        }
                    }
            }
        }
        return new ArrayList<>(map.values());
    }

    private boolean matchProject(String project) {
        if (this.project == null || this.project.length == 0) {
            return true;
        }
        for (String p : this.project) {
            if (p.equals(project)) {
                return true;
            }
        }
        return false;
    }

    static List<List<String>> cartesianProduct(List<List<String>> values) {
        if (values.size() < 2) {
            return new ArrayList<>(values);
        }
        return cartesianProductImpl(0, values);
    }

    private static List<List<String>> cartesianProductImpl(int index, List<List<String>> values) {
        List<List<String>> ret = new ArrayList<>();
        if (index == values.size()) {
            ret.add(new ArrayList<>());
        } else {
            for (String obj : values.get(index)) {
                for (List<String> set : cartesianProductImpl(index + 1, values)) {
                    set.add(0, obj); // needs to be prepended so the order of variants is preserved
                    ret.add(set);
                }
            }
        }
        return ret;
    }

    static int getLongest(Iterable<? extends Spec> l) {
        int max = Integer.MIN_VALUE;
        for (Object o : l) {
            if (o.toString().length() > max) {
                max = o.toString().length();
            }
        }
        return max;
    }

    static String fill(String s, int l, String by) {
        StringBuilder filledString = new StringBuilder(s);
        while (filledString.length() < l) {
            filledString.append(by);
        }
        return filledString.toString();
    }

    public String printMatrix(int orientation, boolean dropRows, boolean dropColumns, Formatter tf) throws StorageException, ManagementException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final String utf8 = StandardCharsets.UTF_8.name();
        List<BuildSpec> bs = getBuilds();
        List<TestSpec> ts = getTests();
        try (final PrintStream stream = new PrintStream(baos, true, utf8)) {
            if (orientation <= 0) {
                printMatrix(stream, bs, ts, dropRows, dropColumns, tf, false);
            }
            if (orientation >= 1) {
                printMatrix(stream, bs, ts, dropRows, dropColumns, tf, true);
            }
            return baos.toString(utf8);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    void printMatrix(
            final PrintStream p,
            final Collection<BuildSpec> buildSpecs,
            final Collection<TestSpec> testSpecs,
            final boolean dropRows,
            final boolean dropColumns,
            final Formatter tf
    ) throws ManagementException, StorageException {
        printMatrix(p, buildSpecs, testSpecs, dropRows, dropColumns, tf, false);
    }

    void printMatrix(
            final PrintStream p,
            final Collection<BuildSpec> buildSpecs,
            final Collection<TestSpec> testSpecs,
            final boolean dropRows,
            final boolean dropColumns,
            final Formatter tf,
            final boolean inverted
    ) throws ManagementException, StorageException {
        int lrow;
        int lcol;
        // TODO: to this better
        // don't print whitespaces when html formatter is used
        if (!(tf instanceof PlainTextFormatter)) {
            lrow = 0;
            lcol = 0;
        } else if (inverted) {
            lrow = getLongest(testSpecs) + 1;
            lcol = getLongest(buildSpecs) + 1;
        } else {
            lrow = getLongest(testSpecs) + 1;
            lcol = getLongest(buildSpecs) + 1;
        }
        List<List<Cell>> matrix = generateMatrix(buildSpecs, testSpecs, dropRows, dropColumns, inverted);
        p.print(printMatrix(matrix, tf, lrow, lcol, buildSpecs.size() * testSpecs.size()));
    }

    String printMatrix(
            final List<List<Cell>> matrix,
            final Formatter formatter,
            final int lrow,
            final int lcol,
            final int total
    ) {
        final StringBuilder output = new StringBuilder(formatter.tableStart());
        int count = 0;
        final List<Cell> firstRow = matrix.get(0);
        final UpperCornerCell upperCornerCell = (UpperCornerCell) firstRow.get(0);
        final String upperCornerCellContent = fill(formatter.upperCorner(upperCornerCell), lrow, " ");
        final StringBuilder firstRowString = new StringBuilder();
        for (int colIdx = 1; colIdx < firstRow.size(); colIdx++) {
            int maxForSpan = 1;
            for (List<Cell> rowForSpan : matrix) {
                maxForSpan = Math.max(rowForSpan.get(colIdx).size(), maxForSpan);
            }
            final TitleCell titleCell = (TitleCell) firstRow.get(colIdx);
            firstRowString.append(fill(formatter.edge(titleCell, maxForSpan), lcol, " "));
        }
        output.append(formatter.rowStart())
                .append(upperCornerCellContent)
                .append(firstRowString)
                .append(upperCornerCellContent)
                .append(formatter.rowEnd());
        final StringBuilder matrixBody = new StringBuilder();
        for (int rowIdx = 1; rowIdx < matrix.size(); rowIdx++) {
            List<Cell> row = matrix.get(rowIdx);
            final TitleCell titleCell = (TitleCell) row.get(0);
            final String rowTitle = titleCell.getTitle();
            final String rowEdge = fill(formatter.edge(titleCell, 1), lrow, " ");
            matrixBody.append(formatter.rowStart()).append(rowEdge);
            for (int colIdx = 1; colIdx < row.size(); colIdx++) {
                final TitleCell colTitleCell = (TitleCell) matrix.get(0).get(colIdx);
                final String colTitle = colTitleCell.getTitle();
                int maxForSpan = 1;
                for (List<Cell> rowForSpan : matrix) {
                    maxForSpan = Math.max(rowForSpan.get(colIdx).size(), maxForSpan);
                }
                final CellGroup cellGroup = (CellGroup) row.get(colIdx);
                matrixBody.append(fill(formatter.cells(cellGroup, maxForSpan, rowTitle, colTitle), lcol, " "));
                count += cellGroup.size();
            }
            matrixBody.append(rowEdge).append(formatter.rowEnd());
        }
        final String lowerCornerCell = fill(formatter.lowerCorner(count, total), lrow, " ");
        return output
                .append(matrixBody)
                .append(formatter.rowStart())
                .append(lowerCornerCell)
                .append(firstRowString)
                .append(lowerCornerCell)
                .append(formatter.rowEnd())
                .append(formatter.tableEnd())
                .toString();
    }

    private List<List<Cell>> generateMatrix(
            final Collection<BuildSpec> buildSpecs,
            final Collection<TestSpec> testSpecs,
            final boolean dropRows,
            final boolean dropColumns,
            final boolean inverted
    ) throws ManagementException, StorageException {
        final Collection<? extends Spec> rows;
        final Collection<? extends Spec> columns;
        if (inverted) {
            rows = testSpecs;
            columns = buildSpecs;
        } else {
            rows = buildSpecs;
            columns = testSpecs;
        }

        List<List<Cell>> listOfRows = new ArrayList<>(rows.size() + 1);

        List<Cell> initialRow = new ArrayList<>(columns.size() + 1);
        final UpperCornerCell upperCornerCell = createUpperLeftCell();
        initialRow.add(upperCornerCell);
        for (Spec t : columns) {
            initialRow.add(new TitleCell(t.toString()));
        }
        listOfRows.add(initialRow);
        final Map<String, Map<String, CellGroup>> map = generateMap(inverted);
        for (final Spec row : rows) {
            final Map<String, CellGroup> rowsMap = map.get(row.toString());
            final List<Cell> matrixRow = new ArrayList<>(columns.size() + 1);
            final TitleCell titleCell = new TitleCell(row.toString());
            matrixRow.add(titleCell);
            if (rowsMap == null) {
                // empty row
                if (dropRows) {
                    continue;
                }
                for (int i = 0; i < columns.size(); i++) {
                    matrixRow.add(new CellGroup());
                }
            } else {
                for (final Spec col : columns) {
                    final CellGroup cellGroup = rowsMap.get(col.toString());
                    if (cellGroup == null) {
                        // empty cell
                        matrixRow.add(new CellGroup());
                        continue;
                    }
                    final CellGroup filteredCellGroup = new CellGroup(cellGroup.getCells().stream()
                            .distinct()
                            .collect(Collectors.toList()));
                    matrixRow.add(filteredCellGroup);
                }
            }
            listOfRows.add(matrixRow);
        }

        if (dropColumns) {
            //not dropping first and last with headers
            //crawling columns
            for (int j = 1; j < listOfRows.get(0).size(); j++) {
                int total = 0;
                //same skip here, thus skipping the instance of checks
                //crawling rows
                for (int i = 1; i < listOfRows.size(); i++) {
                    total = total + listOfRows.get(i).get(j).size();
                }
                if (total == 0) {
                    deleteClumn(listOfRows, j);
                    j--;
                }
            }
        }
        return listOfRows;
    }

    private List<TaskJob> getJobs() throws StorageException, ManagementException {
        return cache.getProjects()
                .stream()
                .filter(project -> matchProject(project.getId()))
                .map(project -> {
                    try {
                        return settings.getJdkProjectParser().parse(project);
                    } catch (ManagementException | StorageException e) {
                        throw new RuntimeException(e);
                    }
                })
                .flatMap(Set::stream)
                .filter(job -> job instanceof TaskJob)
                .sorted(Comparator.comparing(Job::getName))
                .map(job -> (TaskJob) job)
                .collect(Collectors.toList());
    }

    private Consumer<TaskJob> getJobConsumer(final Map<String, Map<String, CellGroup>> map, final boolean inverted) {
        return job -> {
            final BuildSpec buildSpec;
            final TestSpec testSpec;
            final String projectName = job.getProjectName();
            final Product product = job.getProduct();
            final Task task = job.getTask();

            final Platform.Provider provider = job.getPlatform()
                    .getProviders()
                    .stream()
                    .filter(p -> p.getId().equals(job.getPlatformProvider()))
                    .findFirst()
                    .orElseGet(this::createNoneProvider);
            final List<String> variants = job.getVariants()
                    .entrySet()
                    .stream()
                    .sorted(Comparator.comparing(Map.Entry::getKey))
                    .map(e -> e.getValue().getId())
                    .collect(Collectors.toList());
            if (job instanceof BuildJob) {
                final BuildJob buildJob = (BuildJob) job;
                buildSpec = new BuildSpec(
                        buildJob.getPlatform(),
                        provider,
                        projectName,
                        product,
                        variants,
                        buildFilter
                );
                testSpec = new TestSpec(
                        buildJob.getPlatform(),
                        provider,
                        task,
                        testFilter
                );
            } else if (job instanceof TestJob) {
                final TestJob testJob = (TestJob) job;
                final List<String> buildVariants = testJob.getBuildVariants()
                        .entrySet()
                        .stream()
                        .sorted(Comparator.comparing(Map.Entry::getKey))
                        .map(e -> e.getValue().getId())
                        .collect(Collectors.toList());
                final Platform.Provider buildPlatformProvider = testJob.getBuildPlatform()
                        .getProviders()
                        .stream()
                        .filter(p -> p.getId().equals(testJob.getBuildPlatformProvider()))
                        .findFirst()
                        .orElse(provider);
                testSpec = new TestSpec(
                        testJob.getPlatform(),
                        provider,
                        task,
                        variants,
                        testFilter
                );
                if (testJob.getProjectType() == Project.ProjectType.JDK_TEST_PROJECT) {
                    final BuildJob testOnlyBuildJob = new BuildJob(
                            EXTERNAL_PLATFORM_PROVIDER,
                            projectName,
                            product,
                            testJob.getJdkVersion(),
                            job.getBuildProviders(),
                            cache.getTask("build").orElse(null),
                            testJob.getBuildPlatform(),
                            testJob.getBuildVariants(),
                            null,
                            null
                    );
                    buildSpec = new BuildSpec(
                            testJob.getBuildPlatform(),
                            createExternalProvider(),
                            projectName,
                            product,
                            buildVariants,
                            buildFilter
                    );
                    final TestSpec testBuildSpec = new TestSpec(
                            testOnlyBuildJob.getPlatform(),
                            createExternalProvider(),
                            testOnlyBuildJob.getTask(),
                            testFilter
                    );
                    addJob(map, buildSpec, testBuildSpec, parseExternalJob(testOnlyBuildJob), inverted);
                } else {
                    buildSpec = new BuildSpec(
                            testJob.getBuildPlatform(),
                            buildPlatformProvider,
                            projectName,
                            product,
                            buildVariants,
                            buildFilter
                    );
                }
            } else {
                return;
            }
            addJob(map, buildSpec, testSpec, parseJob(job), inverted);
        };
    }

    private Map<String, Map<String, CellGroup>> generateMap(final boolean inverted) throws ManagementException, StorageException {
        final Map<String, Map<String, CellGroup>> map = new HashMap<>();
        getJobs().forEach(getJobConsumer(map, inverted));
        return map;
    }

    private void addJob(
            final Map<String, Map<String, CellGroup>> map,
            final BuildSpec buildSpec,
            final TestSpec testSpec,
            final TitleCell job,
            final boolean inverted
    ) {
        final String rowKey;
        final String colKey;
        if (inverted) {
            rowKey = testSpec.toString();
            colKey = buildSpec.toString();
        } else {
            rowKey = buildSpec.toString();
            colKey = testSpec.toString();
        }
        final Map<String, CellGroup> rowSpecs;
        if (map.containsKey(rowKey)) {
            rowSpecs = map.get(rowKey);
        } else {
            rowSpecs = new HashMap<>();
            map.put(rowKey, rowSpecs);
        }
        final CellGroup cellGroup;
        if (rowSpecs.containsKey(colKey)) {
            cellGroup = rowSpecs.get(colKey);
        } else {
            cellGroup = new CellGroup();
            rowSpecs.put(colKey, cellGroup);
        }
        cellGroup.add(job);
    }

    private UpperCornerCell createUpperLeftCell() {
        if (project == null || project.length == 0) {
            return new UpperCornerCell(Collections.singletonList(new UrlCell("all projects", settings.getJenkinsUrl())));
        }
        final String projectUrlPrefix;
        try {
            projectUrlPrefix = settings.getJenkins().toURI().resolve("view/~").toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        final String projectUrlSuffix = "#projectstatus";
        return new UpperCornerCell(Arrays.stream(project)
                .map(projectName -> new UrlCell(projectName, projectUrlPrefix + projectName + projectUrlSuffix))
                .collect(Collectors.toList()));
    }

    private MultiUrlCell parseExternalJob(final TaskJob job) {
        final List<String> urls = job.getBuildProviders().stream()
                .map(provider -> provider.getPackageInfoUrl()
                        .replace("%{PORT}", String.valueOf(settings.getFileDownloadPort()))
                        .replace("%{PACKAGE_NAME}", job.getProduct().getPackageName())
                )
                .collect(Collectors.toList());
        return new MultiUrlCell(job.getName(), urls);
    }

    private UrlCell parseJob(final TaskJob job) {
        final String jobName = job.getName();
        try {
            return new UrlCell(jobName, settings.getJenkins().toURI().resolve("job/" + jobName).toString());
        } catch (URISyntaxException e) {
            return new UrlCell(jobName);
        }
    }

    private static void deleteClumn(List<List<Cell>> listOfRows, int j) {
        for (List<Cell> row : listOfRows) {
            row.remove(j);
        }
    }
}

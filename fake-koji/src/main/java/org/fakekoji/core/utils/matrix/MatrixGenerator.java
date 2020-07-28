package org.fakekoji.core.utils.matrix;

import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.jobmanager.ConfigCache;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.model.BuildJob;
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

    public String printMatrix(int orientation, boolean dropRows, boolean dropColumns, TableFormatter tf) throws StorageException, ManagementException {
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
            final TableFormatter tf
    ) throws ManagementException, StorageException {
        printMatrix(p, buildSpecs, testSpecs, dropRows, dropColumns, tf, false);
    }

    void printMatrix(
            final PrintStream p,
            final Collection<BuildSpec> buildSpecs,
            final Collection<TestSpec> testSpecs,
            final boolean dropRows,
            final boolean dropColumns,
            final TableFormatter tf,
            final boolean inverted
    ) throws ManagementException, StorageException {
        int lrow;
        int lcol;
        if (inverted) {
            lrow = getLongest(testSpecs) + 1;
            lcol = getLongest(buildSpecs) + 1;
        } else {
            lrow = getLongest(testSpecs) + 1;
            lcol = getLongest(buildSpecs) + 1;
        }
        int total = 0;
        p.print(tf.tableStart());
        List<List<List<Leaf>>> matrix = generateMatrix(buildSpecs, testSpecs, dropRows, dropColumns, inverted);
        for (int i = 0; i < matrix.size(); i++) {
            List<List<Leaf>> row = matrix.get(i);
            p.print(tf.rowStart());
            for (int j = 0; j < row.size(); j++) {
                int maxForSpan = 1;
                for (int ii = 0; ii < matrix.size(); ii++) {
                    List<List<Leaf>> rowForSpan = matrix.get(ii);
                    maxForSpan = Math.max(rowForSpan.get(j).size(), maxForSpan);
                }
                List<Leaf> cel = row.get(j);
                String cellContent;
                if (cel.size() == 1 && cel.get(0) instanceof LeafTitle) {
                    cellContent = cel.get(0).toString();
                } else {
                    cellContent = tf.getContext(cel, maxForSpan, matrix.get(i).get(0).get(0).toString(), matrix.get(0).get(j).get(0).toString());
                    total += cel.size();
                }
                int align = lcol;
                if (j == 0 || j == row.size() - 1) {
                    align = lrow;
                }
                if (cellContent.isEmpty()) {
                    if (i == 0 && (j == 0 || j == row.size() - 1)) {
                        cellContent = tf.initialCell(project);
                    } else if (i == matrix.size() - 1 && (j == 0 || j == row.size() - 1)) {
                        cellContent = tf.lastCell(total, buildSpecs.size() * testSpecs.size());
                    } else {
                        //filing by soething, as leading spaces can be trimmed on the fly
                        cellContent = fill(cellContent, align - 1, "-") + " ";
                    }
                }
                if (cel.size() == 1 && cel.get(0) instanceof LeafTitle) {
                    p.print(tf.cellStart(maxForSpan) + fill(cellContent, align, " ") + tf.cellEnd());
                } else {
                    p.print(tf.cellStart() + fill(cellContent, align, " ") + tf.cellEnd());
                }
            }
            p.println(tf.rowEnd());
        }
        p.print(tf.tableEnd());
    }

    private List<List<List<Leaf>>> generateMatrix(
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

        //last list is content of single cel
        List<List<List<Leaf>>> listOfRows = new ArrayList<>(rows.size() + 2/*rows headers are additional first and last column*/);

        List<List<Leaf>> initialRow = new ArrayList<>(columns.size() + 2);
        initialRow.add(Collections.singletonList(new LeafTitle("")));//initial empty intersection, upper left corner
        for (Spec t : columns) {
            initialRow.add(Collections.singletonList(new LeafTitle(t.toString())));
        }
        initialRow.add(initialRow.get(0));//last empty intersection, upper right corner, reusing
        listOfRows.add(initialRow);
        final Map<String, Map<String, List<TaskJob>>> map = generateMap(inverted);
        for (final Spec row : rows) {
            final Map<String, List<TaskJob>> rowsMap = map.get(row.toString());
            final List<List<Leaf>> matrixRow = new ArrayList<>(columns.size() + 2);
            matrixRow.add(Collections.singletonList(new LeafTitle(row.toString())));
            if (rowsMap == null) {
                // empty row
                for (int i = 0; i < columns.size(); i++) {
                    matrixRow.add(Collections.emptyList());
                }
            } else {
                for (final Spec col : columns) {
                    final List<TaskJob> jobList = rowsMap.get(col.toString());
                    if (jobList == null) {
                        // empty cell
                        matrixRow.add(Collections.emptyList());
                        continue;
                    }
                    final List<Leaf> matrixCell = jobList.stream()
                            .distinct()
                            .map(job -> new Leaf(job.getName()))
                            .collect(Collectors.toList());
                    matrixRow.add(matrixCell);
                }
            }
            matrixRow.add(matrixRow.get(0));
            listOfRows.add(matrixRow);
        }
        listOfRows.add(new ArrayList<>(initialRow));

        if (dropRows) {
            //not dropping first and last with headers
            for (int i = 1; i < listOfRows.size() - 1; i++) {
                List<List<Leaf>> row = listOfRows.get(i);
                int total = 0;
                //same skip here, thus skipping the instance of checks
                for (int j = 1; j < row.size() - 1; j++) {
                    total = total + row.get(j).size();
                }
                if (total == 0) {
                    listOfRows.remove(i);
                    i--;
                }
            }
        }

        if (dropColumns) {
            //not dropping first and last with headers
            //crawling columns
            for (int j = 1; j < listOfRows.get(0).size() - 1; j++) {
                int total = 0;
                //same skip here, thus skipping the instance of checks
                //crawling rows
                for (int i = 1; i < listOfRows.size() - 1; i++) {
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
                .map(job -> (TaskJob) job)
                .collect(Collectors.toList());
    }

    private Consumer<TaskJob> getJobConsumer(final Map<String, Map<String, List<TaskJob>>> map, final boolean inverted) {
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
                    addJob(map, buildSpec, testBuildSpec, testOnlyBuildJob, inverted);
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
            addJob(map, buildSpec, testSpec, job, inverted);
        };
    }

    private Map<String, Map<String, List<TaskJob>>> generateMap(final boolean inverted) throws ManagementException, StorageException {
        final Map<String, Map<String, List<TaskJob>>> map = new HashMap<>();
        getJobs().forEach(getJobConsumer(map, inverted));
        return map;
    }

    private void addJob(
            final Map<String, Map<String, List<TaskJob>>> map,
            final BuildSpec buildSpec,
            final TestSpec testSpec,
            final TaskJob job,
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
        final Map<String, List<TaskJob>> rowSpecs;
        if (map.containsKey(rowKey)) {
            rowSpecs = map.get(rowKey);
        } else {
            rowSpecs = new HashMap<>();
            map.put(rowKey, rowSpecs);
        }
        final List<TaskJob> jobList;
        if (rowSpecs.containsKey(colKey)) {
            jobList = rowSpecs.get(colKey);
        } else {
            jobList = new ArrayList<>();
            rowSpecs.put(colKey, jobList);
        }
        jobList.add(job);
    }

    private static void deleteClumn(List<List<List<Leaf>>> listOfRows, int j) {
        for (List<List<Leaf>> row : listOfRows) {
            row.remove(j);
        }
    }

    public static class Leaf {

        //TODO: replace by soem structure or other way from which
        //will be able to generate jenkins url or the job itself
        private final String fullPath;

        public Leaf(String fullPath) {
            this.fullPath = fullPath;
        }

        @Override
        public String toString() {
            return fullPath;
        }
    }

    public static class LeafTitle extends Leaf {
        final String simpleTitle;

        LeafTitle(String title) {
            super(null);
            simpleTitle = title;
        }

        @Override
        public String toString() {
            return simpleTitle;
        }
    }
}

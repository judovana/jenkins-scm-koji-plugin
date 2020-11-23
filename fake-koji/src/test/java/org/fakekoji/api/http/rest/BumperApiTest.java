package org.fakekoji.api.http.rest;

import org.fakekoji.DataGenerator;
import org.fakekoji.Utils;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.functional.Result;
import org.fakekoji.jobmanager.BumpResult;
import org.fakekoji.jobmanager.JenkinsCliWrapper;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.model.JobUpdateResult;
import org.fakekoji.jobmanager.model.JobUpdateResults;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.storage.StorageException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.fakekoji.DataGenerator.JRE_SDK;
import static org.fakekoji.DataGenerator.SDK;

public class BumperApiTest {
    private final static String taskVariantId = "newtestvariant";
    private final static String defaultValue = "abcdefgh";
    

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @BeforeClass
    public static void setup() {
        JenkinsCliWrapper.killCli();
    }

    @Test
    public void addBuildVariant() throws IOException, ManagementException, StorageException {
        final DataGenerator.FolderHolder folderHolder = DataGenerator.initFolders(temporaryFolder);
        final AccessibleSettings settings = DataGenerator.getSettings(folderHolder);
        DataGenerator.initBuildsRoot(settings.getDbFileRoot());
        final File jobsRoot = settings.getJenkinsJobsRoot();
        DataGenerator.createProjectJobs(settings);
        final BumperAPI bumperApi = new BumperAPI(settings);
        final Map<String, List<String>> params = Stream.of(new String[][]{
                {"name", taskVariantId},
                {"type", "BUILD" },
                {"defaultValue", defaultValue},
                {"values", defaultValue + ",value1,value2" }
        }).collect(Collectors.toMap(data -> data[0], data -> Collections.singletonList(data[1])));
        final Set<String> taskIds = DataGenerator.getTasks()
                .stream()
                .map(Task::getId)
                .collect(Collectors.toSet());
        final Result<BumpResult, OToolError> result = bumperApi.addTaskVariant(params);
        Assert.assertFalse(result.isError());
        final TaskVariant taskVariant = settings.getConfigManager().taskVariantManager.read(taskVariantId);
        Assert.assertEquals(taskVariantId, taskVariant.getId());
        Assert.assertEquals(defaultValue, taskVariant.getDefaultValue());
        Assert.assertEquals(3, taskVariant.getOrder());
        final JobUpdateResults results = result.getValue().getJobResults();
        Assert.assertEquals(47, results.jobsCreated.size());
        Assert.assertTrue(results.jobsArchived.isEmpty());
        Assert.assertTrue(results.jobsRevived.isEmpty());
        Assert.assertTrue(results.jobsRewritten.isEmpty());
        for (final JobUpdateResult jobResult : results.jobsCreated) {
            final String[] parts = jobResult.message.split(" ");
            final String prevJobName = parts[2];
            final String currJobName = parts[4];
            final String prevJobTaskId = prevJobName.split("-")[0];
            final String currJobTaskId = currJobName.split("-")[0];
            Assert.assertTrue(jobResult.success);
            final String buildVariants = prevJobName.split("-")[4];
            Assert.assertEquals(prevJobTaskId, currJobTaskId);
            Assert.assertTrue(taskIds.contains(prevJobTaskId));
            Assert.assertEquals(prevJobName.replace(buildVariants, buildVariants + "." + defaultValue), currJobName);
            Assert.assertTrue(new File(jobsRoot, currJobName).exists());
            Assert.assertFalse(new File(jobsRoot, prevJobName).exists());
        }
        Assert.assertEquals(
                Utils.readResource("org/fakekoji/api/http/rest/post-add-variant-builds-tree"),
                toTree(settings.getDbFileRoot())
        );
    }

    @Test
    public void addTestVariant() throws IOException, ManagementException, StorageException {
        final DataGenerator.FolderHolder folderHolder = DataGenerator.initFolders(temporaryFolder);
        final AccessibleSettings settings = DataGenerator.getSettings(folderHolder);
        final File jobsRoot = settings.getJenkinsJobsRoot();
        DataGenerator.createProjectJobs(settings);
        final BumperAPI bumperApi = new BumperAPI(settings);
        final Task.Type type = Task.Type.TEST;
        final Map<String, List<String>> params = Stream.of(new String[][]{
                {"name", taskVariantId},
                {"type", type.getValue()},
                {"defaultValue", defaultValue},
                {"values", defaultValue + ",ijklmnopq,rstuvwxyz" }

        }).collect(Collectors.toMap(data -> data[0], data -> Collections.singletonList(data[1])));
        final Result<BumpResult, OToolError> result = bumperApi.addTaskVariant(params);
        Assert.assertFalse(result.isError());
        final JobUpdateResults results = result.getValue().getJobResults();
        final Set<String> testTaskIds = DataGenerator.getTasks()
                .stream()
                .filter(task -> task.getType().equals(Task.Type.TEST))
                .map(Task::getId)
                .collect(Collectors.toSet());
        Assert.assertTrue(settings.getConfigManager().taskVariantManager.contains(taskVariantId));
        final TaskVariant taskVariant = settings.getConfigManager().taskVariantManager.read(taskVariantId);
        Assert.assertEquals(taskVariantId, taskVariant.getId());
        Assert.assertEquals(defaultValue, taskVariant.getDefaultValue());
        Assert.assertEquals(5, taskVariant.getOrder());
        Assert.assertEquals(39, results.jobsCreated.size());
        Assert.assertTrue(results.jobsArchived.isEmpty());
        Assert.assertTrue(results.jobsRevived.isEmpty());
        Assert.assertTrue(results.jobsRewritten.isEmpty());
        for (final JobUpdateResult jobResult : results.jobsCreated) {
            final String[] parts = jobResult.message.split(" ");
            final String prevJobName = parts[2];
            final String currJobName = parts[4];
            final String prevJobTaskId = prevJobName.split("-")[0];
            final String currJobTaskId = currJobName.split("-")[0];
            Assert.assertTrue(jobResult.success);
            Assert.assertEquals(prevJobTaskId, currJobTaskId);
            Assert.assertTrue(testTaskIds.contains(prevJobTaskId));
            Assert.assertEquals(currJobName, prevJobName + "." + defaultValue);
            Assert.assertTrue(new File(jobsRoot, currJobName).exists());
            Assert.assertFalse(new File(jobsRoot, prevJobName).exists());
        }
    }

    @Test
    public void addVariantWithExistingName() throws IOException {
        final DataGenerator.FolderHolder folderHolder = DataGenerator.initFolders(temporaryFolder);
        final AccessibleSettings settings = DataGenerator.getSettings(folderHolder);
        final BumperAPI bumperApi = new BumperAPI(settings);
        DataGenerator.createProjectJobs(settings);
        final Result<BumpResult, OToolError> result = bumperApi.addTaskVariant(createParamsMap(new String[][]{
                {"name", DataGenerator.JVM},
                {"type", Task.Type.BUILD.getValue()},
                {"defaultValue", defaultValue},
                {"values", defaultValue + ",ijklmnopq,rstuvwxyz" }
        }));
        Assert.assertTrue(result.isError());
    }

    @Test
    public void addVariantWithExistingValue() throws IOException {
        final DataGenerator.FolderHolder folderHolder = DataGenerator.initFolders(temporaryFolder);
        final AccessibleSettings settings = DataGenerator.getSettings(folderHolder);
        final BumperAPI bumperApi = new BumperAPI(settings);
        DataGenerator.createProjectJobs(settings);
        final Result<BumpResult, OToolError> result = bumperApi.addTaskVariant(createParamsMap(new String[][]{
                {"name", taskVariantId},
                {"type", Task.Type.BUILD.getValue()},
                {"defaultValue", defaultValue},
                {"values", defaultValue + ",ijklmnopq," + DataGenerator.HOTSPOT}
        }));
        Assert.assertTrue(result.isError());
    }

    @Test
    public void addVariantWithInvalidType() throws IOException {
        final DataGenerator.FolderHolder folderHolder = DataGenerator.initFolders(temporaryFolder);
        final AccessibleSettings settings = DataGenerator.getSettings(folderHolder);
        final BumperAPI bumperApi = new BumperAPI(settings);
        DataGenerator.createProjectJobs(settings);
        final Result<BumpResult, OToolError> result = bumperApi.addTaskVariant(createParamsMap(new String[][]{
                {"name", taskVariantId},
                {"type", "fd" },
                {"defaultValue", defaultValue},
                {"values", defaultValue + ",ijklmnopq,rstuvwxyz" }
        }));
        Assert.assertTrue(result.isError());
    }

    @Test
    public void addVariantWithDuplicateValues() throws IOException {
        final DataGenerator.FolderHolder folderHolder = DataGenerator.initFolders(temporaryFolder);
        final AccessibleSettings settings = DataGenerator.getSettings(folderHolder);
        final BumperAPI bumperApi = new BumperAPI(settings);
        DataGenerator.createProjectJobs(settings);
        final Result<BumpResult, OToolError> result = bumperApi.addTaskVariant(createParamsMap(new String[][]{
                {"name", taskVariantId},
                {"type", Task.Type.BUILD.getValue()},
                {"defaultValue", defaultValue},
                {"values", defaultValue + ",ijklmnopq,rstuvwxyz,rstuvwxyz" }
        }));
        Assert.assertTrue(result.isError());
    }

    @Test
    public void addVariantWithMissingDefaultValueInValues() throws IOException {
        final DataGenerator.FolderHolder folderHolder = DataGenerator.initFolders(temporaryFolder);
        final AccessibleSettings settings = DataGenerator.getSettings(folderHolder);
        final BumperAPI bumperApi = new BumperAPI(settings);
        DataGenerator.createProjectJobs(settings);
        final Result<BumpResult, OToolError> result = bumperApi.addTaskVariant(createParamsMap(new String[][]{
                {"name", taskVariantId},
                {"type", Task.Type.BUILD.getValue()},
                {"defaultValue", defaultValue},
                {"values", "ijklmnopq,rstuvwxyz" }
        }));
        Assert.assertTrue(result.isError());
    }

    @Test
    public void addVariantWithEmptyValues() throws IOException {
        final DataGenerator.FolderHolder folderHolder = DataGenerator.initFolders(temporaryFolder);
        final AccessibleSettings settings = DataGenerator.getSettings(folderHolder);
        final BumperAPI bumperApi = new BumperAPI(settings);
        DataGenerator.createProjectJobs(settings);
        final Result<BumpResult, OToolError> result = bumperApi.addTaskVariant(createParamsMap(new String[][]{
                {"name", taskVariantId},
                {"type", Task.Type.BUILD.getValue()},
                {"defaultValue", defaultValue},
                {"values", "" }
        }));
        Assert.assertTrue(result.isError());
    }

    @Test
    public void addVariantWithMissingName() throws IOException {
        final DataGenerator.FolderHolder folderHolder = DataGenerator.initFolders(temporaryFolder);
        final AccessibleSettings settings = DataGenerator.getSettings(folderHolder);
        final BumperAPI bumperApi = new BumperAPI(settings);
        DataGenerator.createProjectJobs(settings);
        final Result<BumpResult, OToolError> result = bumperApi.addTaskVariant(createParamsMap(new String[][]{
                {"type", Task.Type.BUILD.getValue()},
                {"defaultValue", defaultValue},
                {"values", defaultValue + ",ijklmnopq,rstuvwxyz" }
        }));
        Assert.assertTrue(result.isError());
    }

    @Test
    public void addVariantWithMissingType() throws IOException {
        final DataGenerator.FolderHolder folderHolder = DataGenerator.initFolders(temporaryFolder);
        final AccessibleSettings settings = DataGenerator.getSettings(folderHolder);
        final BumperAPI bumperApi = new BumperAPI(settings);
        DataGenerator.createProjectJobs(settings);
        final Result<BumpResult, OToolError> result = bumperApi.addTaskVariant(createParamsMap(new String[][]{
                {"name", taskVariantId},
                {"defaultValue", defaultValue},
                {"values", defaultValue + ",ijklmnopq,rstuvwxyz" }
        }));
        Assert.assertTrue(result.isError());
    }

    @Test
    public void addVariantWithMissingDefaultValue() throws IOException {
        final DataGenerator.FolderHolder folderHolder = DataGenerator.initFolders(temporaryFolder);
        final AccessibleSettings settings = DataGenerator.getSettings(folderHolder);
        final BumperAPI bumperApi = new BumperAPI(settings);
        DataGenerator.createProjectJobs(settings);
        final Result<BumpResult, OToolError> result = bumperApi.addTaskVariant(createParamsMap(new String[][]{
                {"name", taskVariantId},
                {"type", Task.Type.BUILD.getValue()},
                {"values", defaultValue + ",ijklmnopq,rstuvwxyz" }
        }));
        Assert.assertTrue(result.isError());
    }
    @Test
    public void removeBuildVariant() throws IOException {
        final String taskVariantId = JRE_SDK;
        final DataGenerator.FolderHolder folderHolder = DataGenerator.initFolders(temporaryFolder);
        final AccessibleSettings settings = DataGenerator.getSettings(folderHolder);
        DataGenerator.initBuildsRoot(settings.getDbFileRoot());
        final File jobsRoot = settings.getJenkinsJobsRoot();
        DataGenerator.createProjectJobs(settings);
        final BumperAPI bumperApi = new BumperAPI(settings);
        final Map<String, List<String>> params = Stream.of(new String[][]{
                {"name", taskVariantId},
        }).collect(Collectors.toMap(data -> data[0], data -> Collections.singletonList(data[1])));
        final Set<String> taskIds = DataGenerator.getTasks()
                .stream()
                .map(Task::getId)
                .collect(Collectors.toSet());
        final Result<BumpResult, OToolError> result = bumperApi.removeTaskVariant(params);
        Assert.assertFalse(result.isError());
        Assert.assertFalse(settings.getConfigManager().taskVariantManager.contains(taskVariantId));
        final JobUpdateResults results = result.getValue().getJobResults();
        Assert.assertEquals(47, results.jobsCreated.size());
        Assert.assertTrue(results.jobsArchived.isEmpty());
        Assert.assertTrue(results.jobsRevived.isEmpty());
        Assert.assertTrue(results.jobsRewritten.isEmpty());
        for (final JobUpdateResult jobResult : results.jobsCreated) {
            final String[] parts = jobResult.message.split(" ");
            final String prevJobName = parts[2];
            final String currJobName = parts[4];
            final String prevJobTaskId = prevJobName.split("-")[0];
            final String currJobTaskId = currJobName.split("-")[0];
            Assert.assertTrue(jobResult.success);
            Assert.assertEquals(prevJobTaskId, currJobTaskId);
            Assert.assertTrue(taskIds.contains(prevJobTaskId));
            Assert.assertEquals(prevJobName.replace("." + SDK, ""), currJobName);
            Assert.assertTrue(new File(jobsRoot, currJobName).exists());
            Assert.assertFalse(new File(jobsRoot, prevJobName).exists());
        }
        Assert.assertEquals(
                Utils.readResource("org/fakekoji/api/http/rest/post-remove-variant-builds-tree"),
                toTree(settings.getDbFileRoot())
        );
    }

    private Map<String, List<String>> createParamsMap(final String[][] params) {
        return Stream.of(params).collect(Collectors.toMap(data -> data[0], data -> Collections.singletonList(data[1])));
    }

    private String toTree(final File dir) {
        return toTree(dir, "");
    }

    private String toTree(final File dir, final String prefix) {
        final StringBuilder output = new StringBuilder();
        Arrays.stream(Objects.requireNonNull(dir.listFiles())).sorted().forEach(file -> {
            output.append(prefix).append(file.getName()).append('\n');
            if (file.isDirectory()) {
                output.append(toTree(file, prefix + "\t"));
            }
        });
        return output.toString();
    }
}

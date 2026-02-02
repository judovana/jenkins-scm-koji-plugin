package org.fakekoji.api.http.rest;

import org.fakekoji.DataGenerator;
import org.fakekoji.Utils;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.functional.Result;
import org.fakekoji.jobmanager.bumpers.BumpResult;
import org.fakekoji.jobmanager.JenkinsCliWrapper;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.model.JobUpdateResult;
import org.fakekoji.jobmanager.model.JobUpdateResults;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.storage.StorageException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
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
import static org.fakekoji.api.http.rest.BumperAPI.EXECUTE;

public class BumperApiTest {
    private final static String taskVariantId = "newtestvariant";
    private final static String defaultValue = "abcdefgh";

    @BeforeAll
    public static void setup() {
        JenkinsCliWrapper.killCli();
    }

    @Test
    public void addBuildVariant(@TempDir Path temporaryFolder) throws IOException, ManagementException, StorageException {
        temporaryFolder.toFile().mkdirs();
        final DataGenerator.FolderHolder folderHolder = DataGenerator.initFoldersOnFileRoot(temporaryFolder);
        final AccessibleSettings settings = DataGenerator.getSettings(folderHolder);
        DataGenerator.initBuildsRoot(settings.getDbFileRoot());
        final File jobsRoot = settings.getJenkinsJobsRoot();
        DataGenerator.createProjectJobs(settings);
        final BumperAPI bumperApi = new BumperAPI(settings);
        final Map<String, List<String>> params = Stream.of(new String[][]{
                {"name", taskVariantId},
                {"type", "BUILD"},
                {"defaultValue", defaultValue},
                {EXECUTE, "true"},
                {"values", defaultValue + ",value1,value2"}
        }).collect(Collectors.toMap(data -> data[0], data -> Collections.singletonList(data[1])));
        final Set<String> taskIds = DataGenerator.getTasks()
                .stream()
                .map(Task::getId)
                .collect(Collectors.toSet());
        final Result<BumpResult, OToolError> result = bumperApi.addTaskVariant(params);
        Assertions.assertFalse(result.isError());
        final TaskVariant taskVariant = settings.getConfigManager().taskVariantManager.read(taskVariantId);
        Assertions.assertEquals(taskVariantId, taskVariant.getId());
        Assertions.assertEquals(defaultValue, taskVariant.getDefaultValue());
        Assertions.assertEquals(3, taskVariant.getOrder());
        final JobUpdateResults results = result.getValue().getJobResults();
        Assertions.assertEquals(47, results.jobsCreated.size());
        Assertions.assertTrue(results.jobsArchived.isEmpty());
        Assertions.assertTrue(results.jobsRevived.isEmpty());
        Assertions.assertTrue(results.jobsRewritten.isEmpty());
        for (final JobUpdateResult jobResult : results.jobsCreated) {
            final String[] parts = jobResult.message.split(" ");
            final String prevJobName = parts[2];
            final String currJobName = parts[4];
            final String prevJobTaskId = prevJobName.split("-")[0];
            final String currJobTaskId = currJobName.split("-")[0];
            Assertions.assertTrue(jobResult.success);
            final String buildVariants = prevJobName.split("-")[4];
            Assertions.assertEquals(prevJobTaskId, currJobTaskId);
            Assertions.assertTrue(taskIds.contains(prevJobTaskId));
            Assertions.assertEquals(prevJobName.replace(buildVariants, buildVariants + "." + defaultValue), currJobName);
            Assertions.assertTrue(new File(jobsRoot, currJobName).exists());
            Assertions.assertFalse(new File(jobsRoot, prevJobName).exists());
        }
        Assertions.assertEquals(
                Utils.readResource("org/fakekoji/api/http/rest/post-add-variant-builds-tree"),
                toTree(settings.getDbFileRoot())
        );
    }

    @Test
    public void addTestVariant(@TempDir Path temporaryFolder) throws IOException, ManagementException, StorageException {
        temporaryFolder.toFile().mkdirs();
        final DataGenerator.FolderHolder folderHolder = DataGenerator.initFoldersOnFileRoot(temporaryFolder);
        final AccessibleSettings settings = DataGenerator.getSettings(folderHolder);
        final File jobsRoot = settings.getJenkinsJobsRoot();
        DataGenerator.createProjectJobs(settings);
        final BumperAPI bumperApi = new BumperAPI(settings);
        final Task.Type type = Task.Type.TEST;
        final Map<String, List<String>> params = Stream.of(new String[][]{
                {EXECUTE, "true"},
                {"name", taskVariantId},
                {"type", type.getValue()},
                {"defaultValue", defaultValue},
                {"values", defaultValue + ",ijklmnopq,rstuvwxyz"}

        }).collect(Collectors.toMap(data -> data[0], data -> Collections.singletonList(data[1])));
        final Result<BumpResult, OToolError> result = bumperApi.addTaskVariant(params);
        Assertions.assertFalse(result.isError());
        final JobUpdateResults results = result.getValue().getJobResults();
        final Set<String> testTaskIds = DataGenerator.getTasks()
                .stream()
                .filter(task -> task.getType().equals(Task.Type.TEST))
                .map(Task::getId)
                .collect(Collectors.toSet());
        Assertions.assertTrue(settings.getConfigManager().taskVariantManager.contains(taskVariantId));
        final TaskVariant taskVariant = settings.getConfigManager().taskVariantManager.read(taskVariantId);
        Assertions.assertEquals(taskVariantId, taskVariant.getId());
        Assertions.assertEquals(defaultValue, taskVariant.getDefaultValue());
        Assertions.assertEquals(5, taskVariant.getOrder());
        Assertions.assertEquals(39, results.jobsCreated.size());
        Assertions.assertTrue(results.jobsArchived.isEmpty());
        Assertions.assertTrue(results.jobsRevived.isEmpty());
        Assertions.assertTrue(results.jobsRewritten.isEmpty());
        for (final JobUpdateResult jobResult : results.jobsCreated) {
            final String[] parts = jobResult.message.split(" ");
            final String prevJobName = parts[2];
            final String currJobName = parts[4];
            final String prevJobTaskId = prevJobName.split("-")[0];
            final String currJobTaskId = currJobName.split("-")[0];
            Assertions.assertTrue(jobResult.success);
            Assertions.assertEquals(prevJobTaskId, currJobTaskId);
            Assertions.assertTrue(testTaskIds.contains(prevJobTaskId));
            Assertions.assertEquals(currJobName, prevJobName + "." + defaultValue);
            Assertions.assertTrue(new File(jobsRoot, currJobName).exists());
            Assertions.assertFalse(new File(jobsRoot, prevJobName).exists());
        }
    }

    @Test
    public void addVariantWithExistingName(@TempDir Path temporaryFolder) throws IOException {
        temporaryFolder.toFile().mkdirs();
        final DataGenerator.FolderHolder folderHolder = DataGenerator.initFoldersOnFileRoot(temporaryFolder);
        final AccessibleSettings settings = DataGenerator.getSettings(folderHolder);
        final BumperAPI bumperApi = new BumperAPI(settings);
        DataGenerator.createProjectJobs(settings);
        final Result<BumpResult, OToolError> result = bumperApi.addTaskVariant(createParamsMap(new String[][]{
                {"name", DataGenerator.JVM},
                {"type", Task.Type.BUILD.getValue()},
                {"defaultValue", defaultValue},
                {"values", defaultValue + ",ijklmnopq,rstuvwxyz"}
        }));
        Assertions.assertTrue(result.isError());
    }

    @Test
    public void addVariantWithExistingValue(@TempDir Path temporaryFolder) throws IOException {
        temporaryFolder.toFile().mkdirs();
        final DataGenerator.FolderHolder folderHolder = DataGenerator.initFoldersOnFileRoot(temporaryFolder);
        final AccessibleSettings settings = DataGenerator.getSettings(folderHolder);
        final BumperAPI bumperApi = new BumperAPI(settings);
        DataGenerator.createProjectJobs(settings);
        final Result<BumpResult, OToolError> result = bumperApi.addTaskVariant(createParamsMap(new String[][]{
                {"name", taskVariantId},
                {"type", Task.Type.BUILD.getValue()},
                {"defaultValue", defaultValue},
                {"values", defaultValue + ",ijklmnopq," + DataGenerator.HOTSPOT}
        }));
        Assertions.assertTrue(result.isError());
    }

    @Test
    public void addVariantWithInvalidType(@TempDir Path temporaryFolder) throws IOException {
        temporaryFolder.toFile().mkdirs();
        final DataGenerator.FolderHolder folderHolder = DataGenerator.initFoldersOnFileRoot(temporaryFolder);
        final AccessibleSettings settings = DataGenerator.getSettings(folderHolder);
        final BumperAPI bumperApi = new BumperAPI(settings);
        DataGenerator.createProjectJobs(settings);
        final Result<BumpResult, OToolError> result = bumperApi.addTaskVariant(createParamsMap(new String[][]{
                {"name", taskVariantId},
                {"type", "fd"},
                {"defaultValue", defaultValue},
                {"values", defaultValue + ",ijklmnopq,rstuvwxyz"}
        }));
        Assertions.assertTrue(result.isError());
    }

    @Test
    public void addVariantWithDuplicateValues(@TempDir Path temporaryFolder) throws IOException {
        temporaryFolder.toFile().mkdirs();
        final DataGenerator.FolderHolder folderHolder = DataGenerator.initFoldersOnFileRoot(temporaryFolder);
        final AccessibleSettings settings = DataGenerator.getSettings(folderHolder);
        final BumperAPI bumperApi = new BumperAPI(settings);
        DataGenerator.createProjectJobs(settings);
        final Result<BumpResult, OToolError> result = bumperApi.addTaskVariant(createParamsMap(new String[][]{
                {"name", taskVariantId},
                {"type", Task.Type.BUILD.getValue()},
                {"defaultValue", defaultValue},
                {"values", defaultValue + ",ijklmnopq,rstuvwxyz,rstuvwxyz"}
        }));
        Assertions.assertTrue(result.isError());
    }

    @Test
    public void addVariantWithMissingDefaultValueInValues(@TempDir Path temporaryFolder) throws IOException {
        temporaryFolder.toFile().mkdirs();
        final DataGenerator.FolderHolder folderHolder = DataGenerator.initFoldersOnFileRoot(temporaryFolder);
        final AccessibleSettings settings = DataGenerator.getSettings(folderHolder);
        final BumperAPI bumperApi = new BumperAPI(settings);
        DataGenerator.createProjectJobs(settings);
        final Result<BumpResult, OToolError> result = bumperApi.addTaskVariant(createParamsMap(new String[][]{
                {"name", taskVariantId},
                {"type", Task.Type.BUILD.getValue()},
                {"defaultValue", defaultValue},
                {"values", "ijklmnopq,rstuvwxyz"}
        }));
        Assertions.assertTrue(result.isError());
    }

    @Test
    public void addVariantWithEmptyValues(@TempDir Path temporaryFolder) throws IOException {
        temporaryFolder.toFile().mkdirs();
        final DataGenerator.FolderHolder folderHolder = DataGenerator.initFoldersOnFileRoot(temporaryFolder);
        final AccessibleSettings settings = DataGenerator.getSettings(folderHolder);
        final BumperAPI bumperApi = new BumperAPI(settings);
        DataGenerator.createProjectJobs(settings);
        final Result<BumpResult, OToolError> result = bumperApi.addTaskVariant(createParamsMap(new String[][]{
                {"name", taskVariantId},
                {"type", Task.Type.BUILD.getValue()},
                {"defaultValue", defaultValue},
                {"values", ""}
        }));
        Assertions.assertTrue(result.isError());
    }

    @Test
    public void addVariantWithMissingName(@TempDir Path temporaryFolder) throws IOException {
        temporaryFolder.toFile().mkdirs();
        final DataGenerator.FolderHolder folderHolder = DataGenerator.initFoldersOnFileRoot(temporaryFolder);
        final AccessibleSettings settings = DataGenerator.getSettings(folderHolder);
        final BumperAPI bumperApi = new BumperAPI(settings);
        DataGenerator.createProjectJobs(settings);
        final Result<BumpResult, OToolError> result = bumperApi.addTaskVariant(createParamsMap(new String[][]{
                {"type", Task.Type.BUILD.getValue()},
                {"defaultValue", defaultValue},
                {"values", defaultValue + ",ijklmnopq,rstuvwxyz"}
        }));
        Assertions.assertTrue(result.isError());
    }

    @Test
    public void addVariantWithMissingType(@TempDir Path temporaryFolder) throws IOException {
        temporaryFolder.toFile().mkdirs();
        final DataGenerator.FolderHolder folderHolder = DataGenerator.initFoldersOnFileRoot(temporaryFolder);
        final AccessibleSettings settings = DataGenerator.getSettings(folderHolder);
        final BumperAPI bumperApi = new BumperAPI(settings);
        DataGenerator.createProjectJobs(settings);
        final Result<BumpResult, OToolError> result = bumperApi.addTaskVariant(createParamsMap(new String[][]{
                {"name", taskVariantId},
                {"defaultValue", defaultValue},
                {"values", defaultValue + ",ijklmnopq,rstuvwxyz"}
        }));
        Assertions.assertTrue(result.isError());
    }

    @Test
    public void addVariantWithMissingDefaultValue(@TempDir Path temporaryFolder) throws IOException {
        temporaryFolder.toFile().mkdirs();
        final DataGenerator.FolderHolder folderHolder = DataGenerator.initFoldersOnFileRoot(temporaryFolder);
        final AccessibleSettings settings = DataGenerator.getSettings(folderHolder);
        final BumperAPI bumperApi = new BumperAPI(settings);
        DataGenerator.createProjectJobs(settings);
        final Result<BumpResult, OToolError> result = bumperApi.addTaskVariant(createParamsMap(new String[][]{
                {"name", taskVariantId},
                {"type", Task.Type.BUILD.getValue()},
                {"values", defaultValue + ",ijklmnopq,rstuvwxyz"}
        }));
        Assertions.assertTrue(result.isError());
    }

    @Test
    public void removeBuildVariant(@TempDir Path temporaryFolder) throws IOException {
        temporaryFolder.toFile().mkdirs();
        final String taskVariantId = JRE_SDK;
        final DataGenerator.FolderHolder folderHolder = DataGenerator.initFoldersOnFileRoot(temporaryFolder);
        final AccessibleSettings settings = DataGenerator.getSettings(folderHolder);
        DataGenerator.initBuildsRoot(settings.getDbFileRoot());
        final File jobsRoot = settings.getJenkinsJobsRoot();
        DataGenerator.createProjectJobs(settings);
        final BumperAPI bumperApi = new BumperAPI(settings);
        final Map<String, List<String>> params = Stream.of(new String[][]{
                {"name", taskVariantId},
                {EXECUTE, "true"},
        }).collect(Collectors.toMap(data -> data[0], data -> Collections.singletonList(data[1])));
        final Set<String> taskIds = DataGenerator.getTasks()
                .stream()
                .map(Task::getId)
                .collect(Collectors.toSet());
        final Result<BumpResult, OToolError> result = bumperApi.removeTaskVariant(params);
        Assertions.assertFalse(result.isError());
        Assertions.assertFalse(settings.getConfigManager().taskVariantManager.contains(taskVariantId));
        final JobUpdateResults results = result.getValue().getJobResults();
        Assertions.assertEquals(47, results.jobsCreated.size());
        Assertions.assertTrue(results.jobsArchived.isEmpty());
        Assertions.assertTrue(results.jobsRevived.isEmpty());
        Assertions.assertTrue(results.jobsRewritten.isEmpty());
        for (final JobUpdateResult jobResult : results.jobsCreated) {
            final String[] parts = jobResult.message.split(" ");
            final String prevJobName = parts[2];
            final String currJobName = parts[4];
            final String prevJobTaskId = prevJobName.split("-")[0];
            final String currJobTaskId = currJobName.split("-")[0];
            Assertions.assertTrue(jobResult.success);
            Assertions.assertEquals(prevJobTaskId, currJobTaskId);
            Assertions.assertTrue(taskIds.contains(prevJobTaskId));
            Assertions.assertEquals(prevJobName.replace("." + SDK, ""), currJobName);
            Assertions.assertTrue(new File(jobsRoot, currJobName).exists());
            Assertions.assertFalse(new File(jobsRoot, prevJobName).exists());
        }
        Assertions.assertEquals(
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

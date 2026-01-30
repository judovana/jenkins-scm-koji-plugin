package org.fakekoji.jobmanager.project;

import org.fakekoji.DataGenerator;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.functional.Result;
import org.fakekoji.jobmanager.model.Project;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ReverseJDKProjectParserTest {

    @TempDir
    static Path temporaryFolder;

    private AccessibleSettings settings;


    @BeforeEach
    public void setup() throws IOException {
        settings = DataGenerator.getSettings(DataGenerator.initFoldersFromTmpFolder(temporaryFolder.toFile()));
    }

    @Test
    public void parseJDKTestProjectJobs() {
        final ReverseJDKProjectParser parser = settings.getReverseJDKProjectParser();
        final Result<Project, String> result = parser.parseJobs(DataGenerator.getJDKTestProjectJobs());
        Assertions.assertEquals(
                DataGenerator.getJDKTestProject(),
                result.getValue()
        );
    }

    @Test
    public void parseJDKProjectJobs() {
        final ReverseJDKProjectParser parser = settings.getReverseJDKProjectParser();
        final Result<Project, String> result = parser.parseJobs(DataGenerator.getJDKProjectJobs());
        Assertions.assertEquals(
                DataGenerator.getJDKProject(),
                result.getValue()
        );
    }

    @Test
    public void findOrCreateString() {
        final String a = "aaa";
        final String b = "bbb";
        final String c = "ccc";
        final Set<String> strings = new HashSet<>(Arrays.asList(a, b));

        final Optional<String> opt = ReverseJDKProjectParser.findConfig(strings, s -> s.equals(a));
        Assertions.assertTrue(opt.isPresent());
        Assertions.assertEquals(opt.get(), a);

        final Optional<String> optEmpty = ReverseJDKProjectParser.findConfig(strings, s -> s.equals(c));
        Assertions.assertFalse(optEmpty.isPresent());

        final String string = ReverseJDKProjectParser.findOrCreateConfig(strings, s -> s.equals(c), () -> c);
        Assertions.assertEquals(string, c);
    }
}

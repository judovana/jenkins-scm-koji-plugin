package org.fakekoji.jobmanager.bumpers;

import org.fakekoji.DataGenerator;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.functional.Tuple;
import org.fakekoji.jobmanager.bumpers.impl.ProductBumper;
import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.model.JDKVersion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ProductBumperTest {
    
    private AccessibleSettings settings;

    @TempDir
    static Path temporaryFolder;

    @BeforeEach
    public void setup() throws IOException {
        settings = DataGenerator.getSettings(temporaryFolder);
    }

    @Test
    public void bumpJDKProjectJobsWithoutMatch() {
        final Set<Job> jobs = DataGenerator.getJDKProjectJobs();
        final String fromPackage = DataGenerator.getJDK11Product().getPackageName();
        final String toPackage = DataGenerator.getJDK8Product().getPackageName();
        final JDKVersion fromJDKVersion = DataGenerator.getJDKVersion11();
        final JDKVersion toJDKVersion = DataGenerator.getJDKVersion8();
        final ProductBumper bumper = new ProductBumper(settings, fromPackage, toPackage, fromJDKVersion, toJDKVersion);
        final Set<Tuple<Job, Optional<Job>>> tuples = jobs.stream()
                .map(bumper.getTransformFunction())
                .collect(Collectors.toSet());
        Assertions.assertEquals(0, tuples.stream().filter(tuple -> tuple.y.isPresent()).count());
        Assertions.assertEquals(jobs, tuples.stream().map(tuple -> tuple.y.orElse(tuple.x)).collect(Collectors.toSet()));
    }

    @Test
    public void bumpJDKProjectJobsWithMatch() {
        final String fromPackage = DataGenerator.getJDK8Product().getPackageName();
        final String toPackage = DataGenerator.getJDK11Product().getPackageName();
        final JDKVersion fromJDKVersion = DataGenerator.getJDKVersion8();
        final JDKVersion toJDKVersion = DataGenerator.getJDKVersion11();
        final Set<Job> jobs = DataGenerator.getJDKProjectJobs();
        final ProductBumper bumper = new ProductBumper(settings, fromPackage, toPackage, fromJDKVersion, toJDKVersion);
        final Set<Tuple<Job, Optional<Job>>> tuples = jobs.stream()
                .map(bumper.getTransformFunction())
                .collect(Collectors.toSet());
        Assertions.assertEquals(5, tuples.stream().filter(tuple -> tuple.y.isPresent()).count());
        Assertions.assertTrue(tuples.stream().allMatch(isOk(fromPackage, toPackage, fromJDKVersion, toJDKVersion)));
    }

    @Test
    public void bumpJDKTestProjectJobsWithoutMatch() {
        final String fromPackage = DataGenerator.getJDK11Product().getPackageName();
        final String toPackage = DataGenerator.getJDK8Product().getPackageName();
        final JDKVersion fromJDKVersion = DataGenerator.getJDKVersion11();
        final JDKVersion toJDKVersion = DataGenerator.getJDKVersion8();
        final Set<Job> jobs = DataGenerator.getJDKTestProjectJobs();
        final ProductBumper bumper = new ProductBumper(settings, fromPackage, toPackage, fromJDKVersion, toJDKVersion);
        final Set<Tuple<Job, Optional<Job>>> tuples = jobs.stream()
                .map(bumper.getTransformFunction())
                .collect(Collectors.toSet());
        Assertions.assertEquals(0, tuples.stream().filter(tuple -> tuple.y.isPresent()).count());
        Assertions.assertEquals(jobs, tuples.stream().map(tuple -> tuple.y.orElse(tuple.x)).collect(Collectors.toSet()));
    }

    @Test
    public void bumpJDKTestProjectJobsWithMatch() {
        final String fromPackage = DataGenerator.getJDK8Product().getPackageName();
        final String toPackage = DataGenerator.getJDK11Product().getPackageName();
        final JDKVersion fromJDKVersion = DataGenerator.getJDKVersion8();
        final JDKVersion toJDKVersion = DataGenerator.getJDKVersion11();
        final Set<Job> jobs = DataGenerator.getJDKTestProjectJobs();
        final ProductBumper bumper = new ProductBumper(settings, fromPackage, toPackage, fromJDKVersion, toJDKVersion);
        final Set<Tuple<Job, Optional<Job>>> tuples = jobs.stream()
                .map(bumper.getTransformFunction())
                .collect(Collectors.toSet());
        Assertions.assertEquals(5, tuples.stream().filter(tuple -> tuple.y.isPresent()).count());
        Assertions.assertTrue(tuples.stream().allMatch(isOk(fromPackage, toPackage, fromJDKVersion, toJDKVersion)));
    }

    private Predicate<Tuple<Job, Optional<Job>>> isOk(
            final String from,
            final String to,
            final JDKVersion fromJDK,
            final JDKVersion toJDK
    ) {
        return tuple -> {
            final Job original = tuple.x;
            if (original.getProduct().getPackageName().equals(from) && original.getJdkVersion().equals(fromJDK)) {
                if (!tuple.y.isPresent()) {
                    return false;
                }
                final Job bumped = tuple.y.get();
                return bumped.getProduct().getPackageName().equals(to) && bumped.getJdkVersion().equals(toJDK);
            }
            return true;
        };
    }
}

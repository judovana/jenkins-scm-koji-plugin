package org.fakekoji.http;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ProjectMappingFileSystemTest {

    private static final String JAVA7 = "java-1.7.0-openjdk";
    private static final String JAVA8 = "java-1.8.0-openjdk";
    private static final String JAVA9 = "java-9-openjdk";
    private static final String JAVA10 = "java-10-openjdk";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private static final String[] products = {
            "java-X-openjdk",
            "java-openjdk",
            "java-10-openjdk",
            "java-9-openjdk",
            "java-1.8.0-openjdk",
            "java-1.7.0-openjdk"
    };

    private static final String[] projects = {
            "java-10-openjdk",
            "java-1.7.0-openjdk",
            "java-1.7.0-openjdk-forest",
            "java-1.7.0-openjdk-forest-26",
            "java-1.8.0-openjdk",
            "java-1.8.0-openjdk-aarch64",
            "java-1.8.0-openjdk-aarch64-shenandoah",
            "java-1.8.0-openjdk-dev",
            "java-1.8.0-openjdk-shenandoah",
            "java-9-openjdk",
            "java-9-openjdk-dev",
            "java-9-openjdk-shenandoah",
            "java-9-openjdk-updates",
            "java-X-openjdk"
    };

    private static final List<String> nvras = Arrays.asList(
            "java-X-openjdk-jdk.11.4-50.upstream.src.tarxz",
            "java-10-openjdk-jdk.10.46-0.static.x86_64.tarxz",
            "java-1.8.0-openjdk-aarch64.jdk8u152.b16-6.aarch64.static.x86_64.tarxz",
            "java-1.8.0-openjdk-aarch64.shenandoah.jdk8u141.b16.shenandoah.merge.2017.07.27.02-0.aarch64.shenandoah.static.x86_64.tarxz",
            "java-1.7.0-openjdk-icedtea.2.7.0pre10-3.forest.static.x86_64.tarxz",
            "java-1.7.0-openjdk-icedtea.2.6.12-134.forest.26.static.x86_64.tarxz",
            "java-openjdk-10.0.0.46-8.fc27.x86_64.rpm",
            "java-9-openjdk-jdk.9.181-794.shenandoah.static.fastdebug.aarch64.tarxz",
            "java-10-openjdk-jdk.10.46-0.static.fastdebug.i686.tarxz",
            "java-X-openjdk-jdk.11.2-52.static.fastdebug.x86_64.tarxz",
            "java-9-openjdk-9.0.1.11-2.el7.x86_64.rpm",
            "java-9-openjdk-jdk.9.165-31.shenandoah.static.x86_64.tarxz",
            "java-1.8.0-openjdk-jdk8u162.b01-2.static.fastdebug.i686.tarxz",
            "java-1.7.0-openjdk-jdk7u161.b01-0.static.fastdebug.i686.tarxz"
    );

    private static AccessibleSettings settings;

    @BeforeClass
    public static void setUp() {
        try {
            File baseDir = new File(System.getProperty("java.io.tmpdir"));

            File productFile = new File(baseDir, "builds");
            productFile.mkdir();
            File reposFile = new File(baseDir, "repos");
            reposFile.mkdir();

            for (String product : products) {
                new File(productFile, product).mkdir();
            }

            for (String project : projects) {
                new File(reposFile, project).mkdir();
            }

            settings = new AccessibleSettings(productFile, reposFile, 0, 0, 0, 0, 0);
            Arrays.sort(products);
            Arrays.sort(projects);
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    @AfterClass
    public static void tearDown() {
        for (String product : products) {
            new File(settings.getDbFileRoot(), product).deleteOnExit();
        }

        for (String project : projects) {
            new File(settings.getLocalReposRoot(), project).deleteOnExit();
        }
        settings.getDbFileRoot().deleteOnExit();
        settings.getLocalReposRoot().deleteOnExit();
    }

    @Test
    public void getAllProductsTest() throws ProjectMappingExceptions.ProjectMappingException {
        ProjectMapping projectMapping = new ProjectMapping(settings);
        final List<String> actualProducts;
        actualProducts = projectMapping.getAllProducts();
        Collections.sort(actualProducts);
        assertEquals("The actual list of products doesn\'t match the expected", Arrays.asList(products), actualProducts);
    }

    @Test
    public void getAllProjectsTest() throws ProjectMappingExceptions.ProjectMappingException {
        ProjectMapping projectMapping = new ProjectMapping(settings);
        final List<String> actualProjects;
        actualProjects = projectMapping.getAllProjects();
        Collections.sort(actualProjects);
        assertEquals("The actual list of projects doesn\'t match the expected", Arrays.asList(projects), actualProjects);
    }

    @Test
    public void getProjectsOfProductTest() throws ProjectMappingExceptions.ProjectMappingException {
        final List<String> expectedJava7Projects = Arrays.asList(
                "java-1.7.0-openjdk",
                "java-1.7.0-openjdk-forest",
                "java-1.7.0-openjdk-forest-26"
        );
        final List<String> expectedJava8Projects = Arrays.asList(
                "java-1.8.0-openjdk",
                "java-1.8.0-openjdk-aarch64",
                "java-1.8.0-openjdk-aarch64-shenandoah",
                "java-1.8.0-openjdk-dev",
                "java-1.8.0-openjdk-shenandoah"
        );
        final List<String> expectedJava9Projects = Arrays.asList(
                "java-9-openjdk",
                "java-9-openjdk-dev",
                "java-9-openjdk-shenandoah",
                "java-9-openjdk-updates"
        );
        final List<String> expectedJava10Projects = Arrays.asList(
                "java-10-openjdk"
        );

        ProjectMapping projectMapping = new ProjectMapping(settings);
        final List<String> actualJava7Projects;
        actualJava7Projects = projectMapping.getProjectsOfProduct(JAVA7);
        Collections.sort(actualJava7Projects);
        assertEquals("The actual list of projects doesn\'t match the expected", expectedJava7Projects, actualJava7Projects);


        final List<String> actualJava8Projects;
        actualJava8Projects = projectMapping.getProjectsOfProduct(JAVA8);
        Collections.sort(actualJava8Projects);
        assertEquals("The actual list of projects doesn\'t match the expected", expectedJava8Projects, actualJava8Projects);


        final List<String> actualJava9Projects;
        actualJava9Projects = projectMapping.getProjectsOfProduct(JAVA9);
        Collections.sort(actualJava9Projects);
        assertEquals("The actual list of projects doesn\'t match the expected", expectedJava9Projects, actualJava9Projects);


        final List<String> actualJava10Projects;
        actualJava10Projects = projectMapping.getProjectsOfProduct(JAVA10);
        Collections.sort(actualJava10Projects);
        assertEquals("The actual list of projects doesn\'t match the expected", expectedJava10Projects, actualJava10Projects);

        expectedException.expect(ProjectMappingExceptions.ProductDoesNotMatchException.class);
        projectMapping.getProjectsOfProduct("wrong product");
    }

    @Test
    public void getProjectOfNvraTest() throws ProjectMappingExceptions.ProjectMappingException {
        final List<String> expectedProjects = Arrays.asList(
                "java-X-openjdk",
                "java-10-openjdk",
                "java-1.8.0-openjdk-aarch64",
                "java-1.8.0-openjdk-aarch64-shenandoah",
                "java-1.7.0-openjdk-forest",
                "java-1.7.0-openjdk-forest-26",
                "java-10-openjdk",
                "java-9-openjdk-shenandoah",
                "java-10-openjdk",
                "java-X-openjdk",
                "java-9-openjdk",
                "java-9-openjdk-shenandoah",
                "java-1.8.0-openjdk",
                "java-1.7.0-openjdk"
        );

        ProjectMapping projectMapping = new ProjectMapping(settings);
        final List<String> actualProjects = new ArrayList<>();
        for (String nvra : nvras) {
            actualProjects.add(projectMapping.getProjectOfNvra(nvra));

        }
        assertEquals("The actual project doesn\'t match the expected", expectedProjects, actualProjects);

        expectedException.expect(ProjectMappingExceptions.ProjectDoesNotMatchException.class);
        projectMapping.getProjectOfNvra("wrong nvra");
    }

    @Test
    public void getProductOfNvraTest() throws ProjectMappingExceptions.ProjectMappingException {
        final List<String> expectedProducts = Arrays.asList(
                "java-X-openjdk",
                "java-10-openjdk",
                "java-1.8.0-openjdk",
                "java-1.8.0-openjdk",
                "java-1.7.0-openjdk",
                "java-1.7.0-openjdk",
                "java-openjdk",
                "java-9-openjdk",
                "java-10-openjdk",
                "java-X-openjdk",
                "java-9-openjdk",
                "java-9-openjdk",
                "java-1.8.0-openjdk",
                "java-1.7.0-openjdk"
        );

        ProjectMapping projectMapping = new ProjectMapping(settings);
        final List<String> actualProducts = new ArrayList<>();
        for (String nvra : nvras) {
            actualProducts.add(projectMapping.getProductOfNvra(nvra));
        }
        assertEquals("The actual product doesn\'t match the expected", expectedProducts, actualProducts);

        expectedException.expect(ProjectMappingExceptions.ProductDoesNotMatchException.class);
        projectMapping.getProductOfNvra("wrong nvra");
    }

    @Test
    public void getProductOfProjectTest() throws ProjectMappingExceptions.ProjectMappingException {
        final String[] expectedProductsOfProjects = {
                "java-1.7.0-openjdk",
                "java-1.7.0-openjdk",
                "java-1.7.0-openjdk",
                "java-1.8.0-openjdk",
                "java-1.8.0-openjdk",
                "java-1.8.0-openjdk",
                "java-1.8.0-openjdk",
                "java-1.8.0-openjdk",
                "java-10-openjdk",
                "java-9-openjdk",
                "java-9-openjdk",
                "java-9-openjdk",
                "java-9-openjdk",
                "java-X-openjdk"
        };

        ProjectMapping projectMapping = new ProjectMapping(settings);
        final List<String> actualProjects = new ArrayList<>();
        for (String project : projects) {
            actualProjects.add(projectMapping.getProductOfProject(project));
        }
        assertEquals("The actual product doesn\'t match the expected", Arrays.asList(expectedProductsOfProjects), actualProjects);

        expectedException.expect(ProjectMappingExceptions.ProjectDoesNotMatchException.class);
        projectMapping.getProductOfProject("wrong project");
    }
}
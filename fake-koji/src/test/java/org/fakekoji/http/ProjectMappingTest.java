package org.fakekoji.http;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ProjectMappingTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private static final List<String> products = Arrays.asList(
            "java-X-openjdk",
            "java-openjdk",
            "java-10-openjdk",
            "java-9-openjdk",
            "java-1.8.0-openjdk",
            "java-1.7.0-openjdk"
    );

    private static final List<String> projects = Arrays.asList(
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
    );

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

        ProjectMapping projectMapping = new ProjectMapping(null);
        List<String> actualProjects = new ArrayList<>();
        for (String nvra : nvras) {
            actualProjects.add(projectMapping.getProjectOfNvra(nvra, projects));
        }
        actualProjects.sort(String::compareTo);
        expectedProjects.sort(String::compareTo);
        assertEquals("The actual list of projects doesn\'t match the expected", expectedProjects, actualProjects);
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

        ProjectMapping projectMapping = new ProjectMapping(null);
        List<String> actualProducts = new ArrayList<>();
        for (String nvra : nvras) {
            actualProducts.add(projectMapping.getProductOfNvra(nvra, products));
        }
        actualProducts.sort(String::compareTo);
        expectedProducts.sort(String::compareTo);
        assertEquals("The actual list of products doesn\'t match the expected", expectedProducts, actualProducts);

        expectedException.expect(ProjectMappingExceptions.ProductDoesNotMatchException.class);
        projectMapping.getProductOfNvra("wrong nvra", products);
    }

    @Test
    public void getProductOfProjectTest() throws ProjectMappingExceptions.ProjectMappingException {
        final List<String> expectedProductsOfProjects = Arrays.asList(
                "java-10-openjdk",
                "java-1.7.0-openjdk",
                "java-1.7.0-openjdk",
                "java-1.7.0-openjdk",
                "java-1.8.0-openjdk",
                "java-1.8.0-openjdk",
                "java-1.8.0-openjdk",
                "java-1.8.0-openjdk",
                "java-1.8.0-openjdk",
                "java-9-openjdk",
                "java-9-openjdk",
                "java-9-openjdk",
                "java-9-openjdk",
                "java-X-openjdk"
        );

        ProjectMapping projectMapping = new ProjectMapping(null);
        List<String> actualProducts = new ArrayList<>();
        for (String project : projects) {
            actualProducts.add(projectMapping.getProductOfProject(project, products, projects));
        }
        actualProducts.sort(String::compareTo);
        expectedProductsOfProjects.sort(String::compareTo);
        assertEquals("The actual list of products doesn\'t match the expected", expectedProductsOfProjects, actualProducts);

        expectedException.expect(ProjectMappingExceptions.ProjectDoesNotMatchException.class);
        projectMapping.getProductOfProject("wrong project", products, projects);
    }
}
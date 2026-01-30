package org.fakekoji.core.utils.matrix;


import org.fakekoji.DataGenerator;
import org.fakekoji.Utils;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.core.utils.matrix.cell.Cell;
import org.fakekoji.core.utils.matrix.cell.CellGroup;
import org.fakekoji.core.utils.matrix.cell.MultiUrlCell;
import org.fakekoji.core.utils.matrix.cell.TitleCell;
import org.fakekoji.core.utils.matrix.cell.UpperCornerCell;
import org.fakekoji.core.utils.matrix.cell.UrlCell;
import org.fakekoji.core.utils.matrix.formatter.Formatter;
import org.fakekoji.core.utils.matrix.formatter.HtmlFormatter;
import org.fakekoji.core.utils.matrix.formatter.HtmlSpanningFormatter;
import org.fakekoji.core.utils.matrix.formatter.PlainTextFormatter;
import org.fakekoji.jobmanager.ConfigCache;
import org.fakekoji.jobmanager.ConfigManager;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.JDKTestProject;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.storage.StorageException;
import org.junit.Rule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MatrixGeneratorTest {

    private static final String BLANK = "target='_blank'";

    @TempDir
    static Path temporaryFolder;

    private AccessibleSettings settings;
    private final Formatter tf = new PlainTextFormatter();

    @BeforeEach
    public void setup() throws IOException {
        settings = DataGenerator.getSettings(DataGenerator.initFoldersFromTmpFolder(temporaryFolder.toFile()));
    }

    @Test
    public void printSimpleMatrix() {
        final String projectName = DataGenerator.PROJECT_NAME_U;
        final String[] projects = new String[]{projectName};
        MatrixGenerator m = new MatrixGenerator(settings, projects);
        final String url = "https://example.com";
        final List<List<Cell>> matrix = Arrays.asList(
                Arrays.asList(
                        new UpperCornerCell(Collections.singletonList(new UrlCell(projectName))),
                        new TitleCell("Col1"),
                        new TitleCell("Col2"),
                        new TitleCell("Col3")
                ),
                Arrays.asList(
                        new TitleCell("Row1"),
                        new CellGroup(Arrays.asList(
                                new UrlCell("Cell0", url),
                                new UrlCell("Cell1", url),
                                new UrlCell("Cell2", url))
                        ),
                        new CellGroup(Collections.singletonList(new UrlCell("Cell3", url))),
                        new CellGroup(Collections.singletonList(new MultiUrlCell("Cell4", Arrays.asList(url, url))))
                ),
                Arrays.asList(
                        new TitleCell("Row2"),
                        new CellGroup(Arrays.asList(
                                new UrlCell("Cell5", url),
                                new MultiUrlCell("Cell6", Arrays.asList(url, url))
                        )),
                        new CellGroup(Collections.singletonList(new UrlCell("Cell7", url))),
                        new CellGroup(Collections.singletonList(new UrlCell("Cell8", url)))
                )
        );
        final int total = 10;
        final int count = 9;
        final String summary = count + "/" + total;
        final String summaryTd = "<td>" + summary + "</td>";
        final String plainTextOutput = m.printMatrix(matrix, new PlainTextFormatter(), 6, 6, total);
        final String expectedPlainTextOutput = projectName + " Col1  Col2  Col3  " + projectName + " \n"
                + "Row1  3     1     1     Row1  \n"
                + "Row2  2     1     1     Row2  \n"
                + summary + "  Col1  Col2  Col3  " + summary + "  \n";
        Assertions.assertEquals(plainTextOutput, expectedPlainTextOutput);

        final String anchor = "<a "+BLANK+" href=\"" + url + "\">";
        final String expectedHtmlOutput = "<table class=\"resultsTable\" >\n"
                + "<tr><td><a  href=\"#\">" + projectName + "</a></td><td>Col1</td><td>Col2</td><td>Col3</td><td><a  href=\"#\">" + projectName + "</a></td></tr>\n"
                + "<tr><td>Row1</td><td>" + anchor + "[1]</a>" + anchor + "[2]</a>" + anchor + "[3]</a></td><td>" + anchor + "[1]</a></td><td>" + anchor + "[1]</a>" + anchor + "[1]</a></td><td>Row1</td></tr>\n"
                + "<tr><td>Row2</td><td>" + anchor + "[1]</a>" + anchor + "[2]</a>" + anchor + "[2]</a></td><td>" + anchor + "[1]</a></td><td>" + anchor + "[1]</a></td><td>Row2</td></tr>\n"
                + "<tr>" + summaryTd + "<td>Col1</td><td>Col2</td><td>Col3</td>" + summaryTd + "</tr>\n"
                + "</table>";
        final String htmlOutput = m.printMatrix(matrix, new HtmlFormatter(false, projects), 0, 0, total);
        Assertions.assertEquals(expectedHtmlOutput, htmlOutput);

        final String expandedHtmlOutput = m.printMatrix(matrix, new HtmlFormatter(true, projects), 0, 0, total);
        Assertions.assertEquals(expectedHtmlOutput, expandedHtmlOutput);

        final String spanningHtmlOutput = m.printMatrix(matrix, new HtmlSpanningFormatter(true, projects), 0, 0, total);
        final String expectedSpanningHtmlOutput = "<table class=\"resultsTable\" >\n"
                + "<tr><td><a  href=\"#\">uName</a></td><td colspan=\"3\">Col1</td><td>Col2</td><td colspan=\"2\">Col3</td><td><a  href=\"#\">uName</a></td></tr>\n"
                + "<tr><td>Row1</td><td>" + anchor + "[1]</a></td><td>" + anchor + "[2]</a></td><td>" + anchor + "[3]</a></td><td>" + anchor + "[1]</a></td><td>" + anchor + "[1]</a></td><td>" + anchor + "[1]</a></td><td>Row1</td></tr>\n"
                + "<tr><td>Row2</td><td>" + anchor + "[1]</a></td><td>" + anchor + "[2]</a></td><td>" + anchor + "[2]</a></td><td>" + anchor + "[1]</a></td><td>" + anchor + "[1]</a></td><td></td><td>Row2</td></tr>\n"
                + "<tr>" + summaryTd + "<td colspan=\"3\">Col1</td><td>Col2</td><td colspan=\"2\">Col3</td>" + summaryTd + "</tr>\n"
                + "</table>";
        Assertions.assertEquals(expectedSpanningHtmlOutput, spanningHtmlOutput);
    }
    
    @Test
    public void printMatrixWithTwoProjects() {
        final List<String> projectList = Arrays.asList("proj1", "proj2");
        final String[] projects = projectList.toArray(new String[0]);
        MatrixGenerator m = new MatrixGenerator(settings, projects);
        final String url = "https://example.com";
        final List<List<Cell>> matrix = Arrays.asList(
                Arrays.asList(
                        new UpperCornerCell(projectList.stream().map(UrlCell::new).collect(Collectors.toList())),
                        new TitleCell("Col1"),
                        new TitleCell("Col2"),
                        new TitleCell("Col3")
                ),
                Arrays.asList(
                        new TitleCell("Row1"),
                        new CellGroup(Arrays.asList(
                                new UrlCell("Cell0", url),
                                new UrlCell("Cell1", url),
                                new UrlCell("Cell2", url))
                        ),
                        new CellGroup(Collections.singletonList(new UrlCell("Cell3", url))),
                        new CellGroup(Collections.singletonList(new MultiUrlCell("Cell4", Arrays.asList(url, url))))
                ),
                Arrays.asList(
                        new TitleCell("Row2"),
                        new CellGroup(Arrays.asList(
                                new UrlCell("Cell5", url),
                                new MultiUrlCell("Cell6", Arrays.asList(url, url))
                        )),
                        new CellGroup(Collections.singletonList(new UrlCell("Cell7", url))),
                        new CellGroup(Collections.singletonList(new UrlCell("Cell8", url)))
                )
        );
        final int total = 10;
        final int count = 9;
        final String summary = count + "/" + total;
        final String summaryTd = "<td>" + summary + "</td>";

        final String anchor = "<a " + BLANK + " href=\"" + url + "\">";
        final String expectedProjectsCell = "<td>" + projectList.stream()
                .map(proj -> "<a  href=\"#\">" + proj + "</a>")
                .collect(Collectors.joining(" ")) + "</td>";
        final String expectedHtmlOutput = "<table class=\"resultsTable\" >\n"
                + "<tr>" + expectedProjectsCell + "<td>Col1</td><td>Col2</td><td>Col3</td>" + expectedProjectsCell + "</tr>\n"
                + "<tr><td>Row1</td><td>" + anchor + "[1]</a>" + anchor + "[2]</a>" + anchor + "[3]</a></td><td>" + anchor + "[1]</a></td><td>" + anchor + "[1]</a>" + anchor + "[1]</a></td><td>Row1</td></tr>\n"
                + "<tr><td>Row2</td><td>" + anchor + "[1]</a>" + anchor + "[2]</a>" + anchor + "[2]</a></td><td>" + anchor + "[1]</a></td><td>" + anchor + "[1]</a></td><td>Row2</td></tr>\n"
                + "<tr>" + summaryTd + "<td>Col1</td><td>Col2</td><td>Col3</td>" + summaryTd + "</tr>\n"
                + "</table>";
        final String htmlOutput = m.printMatrix(matrix, new HtmlFormatter(false, projects), 0, 0, total);
        Assertions.assertEquals(expectedHtmlOutput, htmlOutput);

        final String expandedHtmlOutput = m.printMatrix(matrix, new HtmlFormatter(true, projects), 0, 0, total);
        Assertions.assertEquals(expectedHtmlOutput, expandedHtmlOutput);

        final String spanningSummaryTd = "<td colspan=\"2\">" + summary + "</td>";
        final String expectedProjectsCells = projectList.stream()
                .map(proj -> "<td><a  href=\"#\">" + proj + "</a></td>")
                .collect(Collectors.joining());
        final String expectedSpanningHtmlOutput = "<table class=\"resultsTable\" >\n"
                + "<tr>" + expectedProjectsCells + "<td colspan=\"3\">Col1</td><td>Col2</td><td colspan=\"2\">Col3</td>" + expectedProjectsCells + "</tr>\n"
                + "<tr><td colspan=\"2\">Row1</td><td>" + anchor + "[1]</a></td><td>" + anchor + "[2]</a></td><td>" + anchor + "[3]</a></td><td>" + anchor + "[1]</a></td><td>" + anchor + "[1]</a></td><td>" + anchor + "[1]</a></td><td colspan=\"2\">Row1</td></tr>\n"
                + "<tr><td colspan=\"2\">Row2</td><td>" + anchor + "[1]</a></td><td>" + anchor + "[2]</a></td><td>" + anchor + "[2]</a></td><td>" + anchor + "[1]</a></td><td>" + anchor + "[1]</a></td><td></td><td colspan=\"2\">Row2</td></tr>\n"
                + "<tr>" + spanningSummaryTd + "<td colspan=\"3\">Col1</td><td>Col2</td><td colspan=\"2\">Col3</td>" + spanningSummaryTd + "</tr>\n"
                + "</table>";
        final String spanningHtmlOutput = m.printMatrix(matrix, new HtmlSpanningFormatter(true, projects), 0, 0, total);
        Assertions.assertEquals(expectedSpanningHtmlOutput, spanningHtmlOutput);
    }

    @Test
    public void showFullMatrix() throws ManagementException, StorageException, IOException {
        MatrixGenerator m = new MatrixGenerator(settings, new String[0]);
        List<BuildSpec> bs = m.getBuilds();
        List<TestSpec> ts = m.getTests();
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final String expectedContent = readResource("full_dropped_matrix");
        m.printMatrix(new PrintStream(outputStream), bs, ts, true, true, tf, true);
        assertContents(expectedContent, outputStream);

    }

    @Test
    public void showFullSqueezedMatrix() throws ManagementException, StorageException, IOException {
        final String expectedContent = readResource("squeezed_matrix");
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final PrintStream printStream = new PrintStream(outputStream);
        MatrixGenerator m = new MatrixGenerator(
                settings,
                ".*",
                ".*",
                ".*",
                new TestEqualityFilter(true, true, false, true, false),
                new BuildEqualityFilter(true, true, false, true, true, false),
                new String[0]
        );
        List<BuildSpec> bs = m.getBuilds();
        List<TestSpec> ts = m.getTests();
        m.printMatrix(printStream, bs, ts, true, true, tf);
        m.printMatrix(printStream, bs, ts, false, false, tf, true);
        assertContents(expectedContent, outputStream);
    }

    @Test
    public void showFilteredSqueezedMatrix() throws ManagementException, StorageException, IOException {
        final String expectedContent = readResource("filtered_matrix");
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final PrintStream printStream = new PrintStream(outputStream);
        MatrixGenerator m = new MatrixGenerator(
                settings,
                ".*",
                ".*",
                ".*slowdebug.*",
                new TestEqualityFilter(true, true, false, true, false),
                new BuildEqualityFilter(true, true, false, true, true, false),
                new String[0]
        );
        List<BuildSpec> bs = m.getBuilds();
        List<TestSpec> ts = m.getTests();
        m.printMatrix(printStream, bs, ts, true, true, tf);
        assertContents(expectedContent, outputStream);
    }

    @Test
    public void showPerProjectMatrix() throws ManagementException, StorageException, IOException {
        final ConfigManager configManager = settings.getConfigManager();
        final ConfigCache configCache = new ConfigCache(configManager);
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final String expectedContent = readResource("per_project_matrices");
        final PrintStream printStream = new PrintStream(outputStream);
        for (Project project : configCache.getProjects()) {
            MatrixGenerator m = new MatrixGenerator(settings, new String[]{project.getId()});
            List<BuildSpec> bs = m.getBuilds();
            List<TestSpec> ts = m.getTests();
            m.printMatrix(printStream, bs, ts, true, true, tf, true);
        }
        assertContents(expectedContent, outputStream);
    }

    @Test
    public void showPerProjectsMatrix() throws ManagementException, StorageException, IOException {
        final ConfigManager configManager = settings.getConfigManager();
        final ConfigCache configCache = new ConfigCache(configManager);
        final List<String> jdkProjectNames = configCache.getJdkProjects().stream().map(JDKProject::getId).sorted().collect(Collectors.toList());
        final List<String> jdkTestProjectNames = configCache.getJdkTestProjects().stream().map(JDKTestProject::getId).sorted().collect(Collectors.toList());
        final List<List<String>> jdkProjectLists = Arrays.asList(jdkProjectNames, jdkTestProjectNames);
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final String expectedContent = readResource("per_projects_matrices");
        final PrintStream printStream = new PrintStream(outputStream);
        for (List<String> projectNames : jdkProjectLists) {
            MatrixGenerator m = new MatrixGenerator(settings, projectNames.toArray(new String[0]));
            List<BuildSpec> bs = m.getBuilds();
            List<TestSpec> ts = m.getTests();
            m.printMatrix(printStream, bs, ts, true, true, tf, true);
        }
        assertContents(expectedContent, outputStream);
    }

    private void assertContents(final String expected, final ByteArrayOutputStream outputStream) throws IOException {
        final String actual = String.join("\n", Utils.readStreamToLines(
                new InputStreamReader(new ByteArrayInputStream(outputStream.toByteArray())),
                String::trim
        ));
        Assertions.assertEquals(expected, actual);
    }

    private String readResource(final String name) throws IOException {
        return String.join("\n", Utils.readStreamToLines(
                new InputStreamReader(this.getClass().getResourceAsStream("/org/fakekoji/core/utils/matrix/" + name)),
                String::trim
        ));
    }
}

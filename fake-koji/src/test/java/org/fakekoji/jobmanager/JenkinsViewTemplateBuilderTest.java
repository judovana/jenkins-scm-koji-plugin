package org.fakekoji.jobmanager;

import org.fakekoji.model.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import java.util.*;


public class JenkinsViewTemplateBuilderTest {

    @Test
    public void basicProjectTempalte() throws IOException {
        String name = "ojdk11~udev~upstream";
        JenkinsViewTemplateBuilder template = JenkinsViewTemplateBuilder.getProjectTemplate(name, Optional.empty(), Optional.empty());

        final String expectedTemplate = "<hudson.model.ListView>\n" +
                "    <name>~" + name + "</name>\n" +
                "    <!--<description>\n" +
                "according to http://radargun.github.io/radargun/measuring_performance/understanding_results.html&#xd;\n" +
                "SOME results are  bigger==better bot OTHER TWO are smaller==better&#xd;\n" +
                "namely:&#xd;\n" +
                "  LATENCY - bigger = worse&#xd;\n" +
                "  OTHERS  - bigger = better&#xd;\n" +
                "</description> -->\n" +
                "    <filterExecutors>true</filterExecutors>\n" +
                "    <filterQueue>true</filterQueue>\n" +
                "    <properties class=\"hudson.model.View$PropertyList\"/>\n" +
                "    <jobNames>\n" +
                "        <comparator class=\"hudson.util.CaseInsensitiveComparator\"/>\n" +
                "    </jobNames>\n" +
                "    <jobFilters/>\n" +
                "    <columns>\n" +
                "        <hudson.views.StatusColumn/>\n" +
                "        <hudson.views.WeatherColumn/>\n" +
                "        <hudson.views.JobColumn/>\n" +
                "        <hudson.views.LastSuccessColumn/>\n" +
                "        <hudson.views.LastFailureColumn/>\n" +
                "        <hudson.views.LastDurationColumn/>\n" +
                "        <hudson.views.BuildButtonColumn/>\n" +
                "    </columns>\n" +
                "    <includeRegex>.*-" + name + "-.*|pull-.*-" + name + "</includeRegex>\n" +
                "    <recurse>false</recurse>\n" +
                "</hudson.model.ListView>\n";

        final String actualTemplate = template.expand();

        Assert.assertEquals(expectedTemplate, actualTemplate);
    }

    @Test
    public void basicTaskTempalteWithDefaultColumns() throws IOException {
        String name = "reproducers~security";
        JenkinsViewTemplateBuilder template = JenkinsViewTemplateBuilder.getTaskTemplate(name, Optional.empty(), Optional.empty(), Optional.empty());

        final String expectedTemplate = "<hudson.model.ListView>\n" +
                "    <name>" + name + "</name>\n" +
                "    <!--<description>\n" +
                "according to http://radargun.github.io/radargun/measuring_performance/understanding_results.html&#xd;\n" +
                "SOME results are  bigger==better bot OTHER TWO are smaller==better&#xd;\n" +
                "namely:&#xd;\n" +
                "  LATENCY - bigger = worse&#xd;\n" +
                "  OTHERS  - bigger = better&#xd;\n" +
                "</description> -->\n" +
                "    <filterExecutors>true</filterExecutors>\n" +
                "    <filterQueue>true</filterQueue>\n" +
                "    <properties class=\"hudson.model.View$PropertyList\"/>\n" +
                "    <jobNames>\n" +
                "        <comparator class=\"hudson.util.CaseInsensitiveComparator\"/>\n" +
                "    </jobNames>\n" +
                "    <jobFilters/>\n" +
                "    <columns>\n" +
                "        <hudson.views.StatusColumn/>\n" +
                "        <hudson.views.WeatherColumn/>\n" +
                "        <hudson.views.JobColumn/>\n" +
                "        <hudson.views.LastSuccessColumn/>\n" +
                "        <hudson.views.LastFailureColumn/>\n" +
                "        <hudson.views.LastDurationColumn/>\n" +
                "        <hudson.views.BuildButtonColumn/>\n" +
                "    </columns>\n" +
                "    <includeRegex>" + name + "-.*</includeRegex>\n" +
                "    <recurse>false</recurse>\n" +
                "</hudson.model.ListView>\n";

        final String actualTemplate = template.expand();

        Assert.assertEquals(expectedTemplate, actualTemplate);
    }

    @Test
    public void basicTaskTempalteWithColumns() throws IOException {
        String name = "jcstress";
        JenkinsViewTemplateBuilder template = JenkinsViewTemplateBuilder.getTaskTemplate(name, Optional.of("<myColumn/>"), Optional.empty(), Optional.empty());

        final String expectedTemplate = "<hudson.model.ListView>\n" +
                "    <name>jcstress</name>\n" +
                "    <!--<description>\n" +
                "according to http://radargun.github.io/radargun/measuring_performance/understanding_results.html&#xd;\n" +
                "SOME results are  bigger==better bot OTHER TWO are smaller==better&#xd;\n" +
                "namely:&#xd;\n" +
                "  LATENCY - bigger = worse&#xd;\n" +
                "  OTHERS  - bigger = better&#xd;\n" +
                "</description> -->\n" +
                "    <filterExecutors>true</filterExecutors>\n" +
                "    <filterQueue>true</filterQueue>\n" +
                "    <properties class=\"hudson.model.View$PropertyList\"/>\n" +
                "    <jobNames>\n" +
                "        <comparator class=\"hudson.util.CaseInsensitiveComparator\"/>\n" +
                "    </jobNames>\n" +
                "    <jobFilters/>\n" +
                "    <columns>\n" +
                "        <myColumn/>\n" +
                "    </columns>\n" +
                "    <includeRegex>jcstress-.*</includeRegex>\n" +
                "    <recurse>false</recurse>\n" +
                "</hudson.model.ListView>\n";

        final String actualTemplate = template.expand();

        Assert.assertEquals(expectedTemplate, actualTemplate);
    }


    @Test
    public void basicPlatformTempalte() throws IOException {
        String name = "plat7.form";
        Platform p = new Platform("plat7.form", "plat", "7", "7", "form", "form",
                new ArrayList<Platform.Provider>(), "plat7.form",
                Platform.TestStableYZupdates.NaN, Platform.TestStableYZupdates.NaN,
                new ArrayList<String>(), new ArrayList<OToolVariable>());
        JenkinsViewTemplateBuilder template = JenkinsViewTemplateBuilder.getPlatformTemplate(name, Arrays.asList(p));

        final String expectedTemplate = "<hudson.model.ListView>\n" +
                "    <name>." + name + "</name>\n" +
                "    <!--<description>\n" +
                "according to http://radargun.github.io/radargun/measuring_performance/understanding_results.html&#xd;\n" +
                "SOME results are  bigger==better bot OTHER TWO are smaller==better&#xd;\n" +
                "namely:&#xd;\n" +
                "  LATENCY - bigger = worse&#xd;\n" +
                "  OTHERS  - bigger = better&#xd;\n" +
                "</description> -->\n" +
                "    <filterExecutors>true</filterExecutors>\n" +
                "    <filterQueue>true</filterQueue>\n" +
                "    <properties class=\"hudson.model.View$PropertyList\"/>\n" +
                "    <jobNames>\n" +
                "        <comparator class=\"hudson.util.CaseInsensitiveComparator\"/>\n" +
                "    </jobNames>\n" +
                "    <jobFilters/>\n" +
                "    <columns>\n" +
                "        <hudson.views.StatusColumn/>\n" +
                "        <hudson.views.WeatherColumn/>\n" +
                "        <hudson.views.JobColumn/>\n" +
                "        <hudson.views.LastSuccessColumn/>\n" +
                "        <hudson.views.LastFailureColumn/>\n" +
                "        <hudson.views.LastDurationColumn/>\n" +
                "        <hudson.views.BuildButtonColumn/>\n" +
                "    </columns>\n" +
                "    <includeRegex>.*-" + name + "\\..*</includeRegex>\n" +
                "    <recurse>false</recurse>\n" +
                "</hudson.model.ListView>\n";

        final String actualTemplate = template.expand();

        Assert.assertEquals(expectedTemplate, actualTemplate);
    }

    @Test
    public void basicPlatformTempalteArchNotFound() throws IOException {
        String name = "another8.plat";
        Platform p = new Platform("plat7.form", "plat", "7", "7", "form", "form",
                new ArrayList<Platform.Provider>(), "plat7.form",
                Platform.TestStableYZupdates.NaN, Platform.TestStableYZupdates.NaN,
                new ArrayList<String>(), new ArrayList<OToolVariable>());
        JenkinsViewTemplateBuilder template = JenkinsViewTemplateBuilder.getPlatformTemplate(name, Arrays.asList(p));

        final String expectedTemplate = "<hudson.model.ListView>\n" +
                "    <name>." + name + "</name>\n" +
                "    <!--<description>\n" +
                "according to http://radargun.github.io/radargun/measuring_performance/understanding_results.html&#xd;\n" +
                "SOME results are  bigger==better bot OTHER TWO are smaller==better&#xd;\n" +
                "namely:&#xd;\n" +
                "  LATENCY - bigger = worse&#xd;\n" +
                "  OTHERS  - bigger = better&#xd;\n" +
                "</description> -->\n" +
                "    <filterExecutors>true</filterExecutors>\n" +
                "    <filterQueue>true</filterQueue>\n" +
                "    <properties class=\"hudson.model.View$PropertyList\"/>\n" +
                "    <jobNames>\n" +
                "        <comparator class=\"hudson.util.CaseInsensitiveComparator\"/>\n" +
                "    </jobNames>\n" +
                "    <jobFilters/>\n" +
                "    <columns>\n" +
                "        <hudson.views.StatusColumn/>\n" +
                "        <hudson.views.WeatherColumn/>\n" +
                "        <hudson.views.JobColumn/>\n" +
                "        <hudson.views.LastSuccessColumn/>\n" +
                "        <hudson.views.LastFailureColumn/>\n" +
                "        <hudson.views.LastDurationColumn/>\n" +
                "        <hudson.views.BuildButtonColumn/>\n" +
                "    </columns>\n" +
                "    <includeRegex>another8.plat Is strange as was not found</includeRegex>\n" +
                "    <recurse>false</recurse>\n" +
                "</hudson.model.ListView>\n";

        final String actualTemplate = template.expand();

        Assert.assertEquals(expectedTemplate, actualTemplate);
    }



}

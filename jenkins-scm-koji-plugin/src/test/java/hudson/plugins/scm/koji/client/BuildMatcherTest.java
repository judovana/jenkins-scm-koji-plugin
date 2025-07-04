package hudson.plugins.scm.koji.client;

import hudson.plugins.scm.koji.KojiBuildProvider;
import hudson.plugins.scm.koji.NotProcessedNvrPredicate;
import hudson.plugins.scm.koji.model.Build;
import hudson.plugins.scm.koji.model.BuildProvider;
import hudson.plugins.scm.koji.model.RPM;
import java.io.IOException;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;

public class BuildMatcherTest {

    static KojiBuildProvider createKojiBuildProvider() {
        return new KojiBuildProvider("proc://unused", "proc://unused");
    }

    static List<KojiBuildProvider> createKojiBuildProviders() {
        return Arrays.asList(createKojiBuildProvider());
    }

    static BuildProvider createBuildProvider() {
        return new BuildProvider("proc://unused", "proc://unused");
    }

    static List<BuildProvider> createBuildProviders() {
        return Arrays.asList(createBuildProvider());
    }

    static RPM createRPM(String release) {
        String base = "b-1-" + release;
        return new RPM("b", "1", release, base, "x64", base + ".x64.tgz");
    }

    static Build createBuild(int release, String date) {
        String id = "b-1-" + release;
        return new Build(release,
                "b",
                "1",
                "" + release,
                id,
                date,
                Arrays.asList(createRPM("" + release)),
                new HashSet<>(),
                createBuildProvider(),
                false);
    }

    static String getLocalDate(int mm) {
        return "1000" + sanity(mm);

    }

    static String getIsoDate(int mm, int ss) {
        return "2001-10-11T10:" + sanity(mm) + ":" + sanity(ss) + "+00:00";
    }

    static String getDtfDate(int mm, int ss) {
        return "2001-10-11 10:" + sanity(mm) + ":" + sanity(ss);
    }

    private static String sanity(int mm) {
        if (mm > 99) {
            throw new RuntimeException("to much time");
        }
        if (mm < 10) {
            return "0" + mm;
        } else {
            return "" + mm;
        }
    }

    @Test
    public void singleBuildNoComparsion() throws IOException {
        BuildMatcher bm = new BuildMatcher(createKojiBuildProviders(), NotProcessedNvrPredicate.createNotProcessedNvrPredicate(new ArrayList<>()), 3, null) {
            @Override
            List<Build> getBuilds(BuildProvider buildProvider) {
                return Arrays.asList(createBuild(1, "noDate!cha!"));
            }

            @Override
            Build getBuild(Build build) {
                return build;
            }
        };
        List<Build> l = BuildMatcher.listBuilds(bm, null).collect(Collectors.toList());
        Assert.assertEquals(1, l.size());
        Optional<Build> b = BuildMatcher.getLatestOfNewestBuilds(bm, null);
        Assert.assertEquals("b-1-1", b.get().getNvr());
    }

    @Test(expected = DateTimeParseException.class)
    public void twoBuildsComaprsionError() throws IOException {
        BuildMatcher bm = new BuildMatcher(createKojiBuildProviders(), NotProcessedNvrPredicate.createNotProcessedNvrPredicate(new ArrayList<>()), 3, null) {
            @Override
            List<Build> getBuilds(BuildProvider buildProvider) {
                return Arrays.asList(
                        createBuild(1, "error"),
                        createBuild(2, "error"));
            }

            @Override
            Build getBuild(Build build) {
                return build;
            }
        };
        List<Build> l = BuildMatcher.listBuilds(bm, null).collect(Collectors.toList());
    }

    @Test
    public void twoBuilds() throws IOException {
        BuildMatcher bm = new BuildMatcher(createKojiBuildProviders(), NotProcessedNvrPredicate.createNotProcessedNvrPredicate(new ArrayList<>()), 3, null) {
            @Override
            List<Build> getBuilds(BuildProvider buildProvider) {
                return Arrays.asList(createBuild(1, getDtfDate(10, 10)),
                        createBuild(2, getDtfDate(9, 9)));
            }

            @Override
            Build getBuild(Build build) {
                return build;
            }
        };
        List<Build> l = BuildMatcher.listBuilds(bm, null).collect(Collectors.toList());
        Assert.assertEquals(2, l.size());
        Assert.assertEquals("b-1-1", l.get(0).getNvr());
        Assert.assertEquals("b-1-2", l.get(1).getNvr());
        Optional<Build> b = BuildMatcher.getLatestOfNewestBuilds(bm, null);
        Assert.assertEquals("b-1-2", b.get().getNvr());
    }

    @Test
    public void threeBuilds() throws IOException {
        BuildMatcher bm = new BuildMatcher(createKojiBuildProviders(), NotProcessedNvrPredicate.createNotProcessedNvrPredicate(new ArrayList<>()), 3, null) {
            @Override
            List<Build> getBuilds(BuildProvider buildProvider) {
                return Arrays.asList(createBuild(1, getDtfDate(10, 10)),
                        createBuild(3, getDtfDate(8, 8)),
                        createBuild(2, getDtfDate(9, 9)));
            }

            @Override
            Build getBuild(Build build) {
                return build;
            }
        };
        List<Build> l = BuildMatcher.listBuilds(bm, null).collect(Collectors.toList());
        Assert.assertEquals(3, l.size());
        Assert.assertEquals("b-1-1", l.get(0).getNvr());
        Assert.assertEquals("b-1-2", l.get(1).getNvr());
        Assert.assertEquals("b-1-3", l.get(2).getNvr());
        Optional<Build> b = BuildMatcher.getLatestOfNewestBuilds(bm, null);
        Assert.assertEquals("b-1-3", b.get().getNvr());
    }


     @Test
    public void fourBuilds() throws IOException {
        BuildMatcher bm = new BuildMatcher(createKojiBuildProviders(), NotProcessedNvrPredicate.createNotProcessedNvrPredicate(new ArrayList<>()), 3, null) {
            @Override
            List<Build> getBuilds(BuildProvider buildProvider) {
                return Arrays.asList(createBuild(1, getDtfDate(10, 10)),
                        createBuild(4, getDtfDate(7, 7)),
                        createBuild(3, getDtfDate(8, 8)),
                        createBuild(2, getDtfDate(9, 9)));
            }

            @Override
            Build getBuild(Build build) {
                return build;
            }
        };
        List<Build> l = BuildMatcher.listBuilds(bm, null).collect(Collectors.toList());
        Assert.assertEquals(3, l.size());
        Assert.assertEquals("b-1-1", l.get(0).getNvr());
        Assert.assertEquals("b-1-2", l.get(1).getNvr());
        Assert.assertEquals("b-1-3", l.get(2).getNvr());
        Optional<Build> b = BuildMatcher.getLatestOfNewestBuilds(bm, null);
        Assert.assertEquals("b-1-3", b.get().getNvr());
    }

    @Test
    public void fourBuildsExcludeToOldOne() throws IOException {
        BuildMatcher bm = new BuildMatcher(createKojiBuildProviders(), NotProcessedNvrPredicate.createNotProcessedNvrPredicate(Arrays.asList("b-1-4")), 3, null) {
            @Override
            List<Build> getBuilds(BuildProvider buildProvider) {
                return Arrays.asList(createBuild(1, getDtfDate(10, 10)),
                        createBuild(4, getDtfDate(7, 7)),
                        createBuild(3, getDtfDate(8, 8)),
                        createBuild(2, getDtfDate(9, 9)));
            }

            @Override
            Build getBuild(Build build) {
                return build;
            }
        };
        List<Build> l = BuildMatcher.listBuilds(bm, null).collect(Collectors.toList());
        Assert.assertEquals(3, l.size());
        Assert.assertEquals("b-1-1", l.get(0).getNvr());
        Assert.assertEquals("b-1-2", l.get(1).getNvr());
        Assert.assertEquals("b-1-3", l.get(2).getNvr());
        Optional<Build> b = BuildMatcher.getLatestOfNewestBuilds(bm, null);
        Assert.assertEquals("b-1-3", b.get().getNvr());
    }

    @Test
    public void fourBuildsExcludeOkOne() throws IOException {
        BuildMatcher bm = new BuildMatcher(createKojiBuildProviders(), NotProcessedNvrPredicate.createNotProcessedNvrPredicate(Arrays.asList("b-1-3")), 3, null) {
            @Override
            List<Build> getBuilds(BuildProvider buildProvider) {
                return Arrays.asList(createBuild(1, getDtfDate(10, 10)),
                        createBuild(4, getDtfDate(7, 7)),
                        createBuild(3, getDtfDate(8, 8)),
                        createBuild(2, getDtfDate(9, 9)));
            }

            @Override
            Build getBuild(Build build) {
                return build;
            }
        };
        List<Build> l = BuildMatcher.listBuilds(bm, null).collect(Collectors.toList());
        Assert.assertEquals(2, l.size());
        Assert.assertEquals("b-1-1", l.get(0).getNvr());
        Assert.assertEquals("b-1-2", l.get(1).getNvr());
        Optional<Build> b = BuildMatcher.getLatestOfNewestBuilds(bm, null);
        Assert.assertEquals("b-1-2", b.get().getNvr());
    }
}

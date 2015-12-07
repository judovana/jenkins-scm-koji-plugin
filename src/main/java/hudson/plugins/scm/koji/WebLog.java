package hudson.plugins.scm.koji;

@FunctionalInterface
public interface WebLog {

    void message(String message);

}

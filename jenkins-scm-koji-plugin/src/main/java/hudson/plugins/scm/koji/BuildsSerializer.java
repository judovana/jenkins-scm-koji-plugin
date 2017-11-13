package hudson.plugins.scm.koji;

import hudson.plugins.scm.koji.model.Build;
import hudson.plugins.scm.koji.model.RPM;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.ref.SoftReference;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

public class BuildsSerializer {

    private static SoftReference<JAXBContext> JAXB_CONTEXT_REFERENCE = new SoftReference<>(null);

    private static JAXBContext jaxbContext() {
        try {
            JAXBContext context = JAXB_CONTEXT_REFERENCE.get();
            if (context == null) {
                context = JAXBContext.newInstance(Build.class, RPM.class);
                JAXB_CONTEXT_REFERENCE = new SoftReference<>(context);
            }
            return context;
        } catch (Exception ex) {
            throw new RuntimeException("Exception while initializing JAXB context", ex);
        }
    }

    public Build read(File file) {
        if (!file.exists() || !file.isFile() || file.length() < 1) {
            return null;
        }
        try (Reader reader = new InputStreamReader(new BufferedInputStream(new FileInputStream(file)), "UTF-8")) {
            Object result = jaxbContext().createUnmarshaller().unmarshal(reader);
            if (result == null || result instanceof Build) {
                return (Build) result;
            }
            // if we are still here - something went wrong:
            throw new RuntimeException("Deserialization expected Build but got: " + result);
        } catch (Exception ex) {
            throw new RuntimeException("Exception while reading the build XML", ex);
        }
    }

    public void write(Build build, File file) {
        try (Writer writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(file)), "UTF-8")) {
            Marshaller marshaller = jaxbContext().createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.marshal(build, writer);
        } catch (Exception ex) {
            throw new RuntimeException("Exception while writing the build to file", ex);
        }
    }

}

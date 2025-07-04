/*
 * The MIT License
 *
 * Copyright 2015 user.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.scm.koji;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotProcessedNvrPredicate implements Predicate<String>, java.io.Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(NotProcessedNvrPredicate.class);

    public static Predicate<String> createNotProcessedNvrPredicateFromFile(File processedNvrFile, File globalprocessedNvrFile) throws IOException {
        List<String> singleJobProcessed = fileToList(processedNvrFile);
        List<String> globalProcessed = fileToList(globalprocessedNvrFile);
        List<String> joinedList = new ArrayList<>(singleJobProcessed.size()+globalProcessed.size());
        joinedList.addAll(singleJobProcessed);
        joinedList.addAll(globalProcessed);
        return new NotProcessedNvrPredicate(joinedList);
    }

    @SuppressFBWarnings(value="RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", justification = "Check is odne on bytecode level in try with resources")
    private static List<String> fileToList(File processedNvrFile) throws IOException {    
        if (processedNvrFile!= null && processedNvrFile.exists()) {
            if (processedNvrFile.isFile() && processedNvrFile.canRead()) {
                try (Stream<String> stream = Files.lines(processedNvrFile.toPath(), StandardCharsets.UTF_8)) {
                    return streamToList(stream);
                }
            } else {
                throw new IOException("Processed NVRs is not readable: " + processedNvrFile.getAbsolutePath());
            }
        } else {
            return new ArrayList<>(0);
        }
    }

    public static List<String> streamToList(Stream<String> stream) throws IOException {
        return stream.collect(Collectors.toList());
    }
    
    public static Predicate<String> createNotProcessedNvrPredicate(List<String> stream) {
        return new NotProcessedNvrPredicate(stream);
    }

    private final Set<String> processedNvrs;

    private NotProcessedNvrPredicate(Set<String> processedNvrs) {
        this.processedNvrs = processedNvrs;
    }

    public NotProcessedNvrPredicate(List<String> processedNvrs) {
        Set<String> nvrsSet = new HashSet<>(processedNvrs.size());
        for (String string : processedNvrs) {
            string = string.trim();
            if (string.contains(" ")) {
                string = string.split(" +")[0];
                nvrsSet.add(string);
            } else {
                nvrsSet.add(string);
            }
        }
        this.processedNvrs = nvrsSet;
    }

    @Override
    public boolean test(String nvr) {
        LOG.info("Searching for " + nvr + " in " + processedNvrs.toString());
        boolean result = !processedNvrs.contains(nvr);
        LOG.info("found[" + nvr + "]: " + !result);
        return result;
    }

}

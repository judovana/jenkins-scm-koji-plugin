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
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotProcessedNvrPredicate implements Predicate<String>, java.io.Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(NotProcessedNvrPredicate.class);

    public static Predicate<String> createNotProcessedNvrPredicateFromFile(File processedNvrFile) throws IOException {
        if (processedNvrFile.exists()) {
            if (processedNvrFile.isFile() && processedNvrFile.canRead()) {
                try (Stream<String> stream = Files.lines(processedNvrFile.toPath(), StandardCharsets.UTF_8)) {
                    Set<String> nvrsSet = stream.collect(Collectors.toSet());
                    return new NotProcessedNvrPredicate(nvrsSet);
                }
            } else {
                throw new IOException("Processed NVRs is not readable: " + processedNvrFile.getAbsolutePath());
            }
        } else {
            return new NotProcessedNvrPredicate(new HashSet<>());
        }
    }

    private final Set<String> processedNvrs;

    public NotProcessedNvrPredicate(Set<String> processedNvrs) {
        this.processedNvrs = processedNvrs;
    }

    @Override
    public boolean test(String nvr) {
        LOG.info("Searching for " + nvr + " in " + processedNvrs.toString());
        boolean result = !processedNvrs.contains(nvr);
        LOG.info("found["+nvr+"]: " + !result);
        return result;
    }

}

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

import hudson.FilePath;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.jenkinsci.remoting.RoleChecker;

import static hudson.plugins.scm.koji.Constants.PROCESSED_BUILDS_HISTORY;

public class NotProcessedNvrPredicate implements Predicate<String> {

    private final FilePath workspace;
    private volatile Set<String> processedNvrs;

    public NotProcessedNvrPredicate(FilePath workspace) {
        this.workspace = workspace;
    }

    @Override
    public boolean test(String nvr) {
        Set<String> processed = this.processedNvrs;
        if (processed == null) {
            synchronized (this) {
                processed = this.processedNvrs;
                if (processed == null) {
                    try {
                        processed = workspace.act(new ReadProcessedCallable());
                    } catch (Exception ex) {
                        throw new RuntimeException("Exception while reading '" + PROCESSED_BUILDS_HISTORY + "' file from workspace", ex);
                    }
                    this.processedNvrs = processed;
                }
            }
        }
        return !processed.contains(nvr);
    }

    private Set<String> readProcessedNvrs(File workspace) throws IOException, InterruptedException {
        File processedFile = new File(workspace, PROCESSED_BUILDS_HISTORY);
        if (!processedFile.exists() || !processedFile.isFile() || processedFile.length() < 1) {
            return Collections.emptySet();
        }
        Set<String> set = Files
                .lines(processedFile.toPath(), Charset.forName("UTF-8"))
                .collect(Collectors.toSet());
        return set;
    }

    private class ReadProcessedCallable implements FilePath.FileCallable<Set<String>> {

        @Override
        public Set<String> invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {
            return readProcessedNvrs(workspace);
        }

        @Override
        public void checkRoles(RoleChecker checker) throws SecurityException {
            // TODO maybe implement?
        }

    }

}

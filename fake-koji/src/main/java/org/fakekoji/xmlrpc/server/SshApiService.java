/*
 * The MIT License
 *
 * Copyright 2017 jvanek.
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
package org.fakekoji.xmlrpc.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.sshd.common.SshException;
import org.apache.sshd.common.config.keys.AuthorizedKeyEntry;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.config.keys.PublicKeyEntryResolver;
import org.apache.sshd.common.scp.ScpFileOpener;
import org.apache.sshd.common.scp.ScpSourceStreamResolver;
import org.apache.sshd.common.scp.ScpTargetStreamResolver;
import org.apache.sshd.common.scp.ScpTimestamp;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.session.ServerSession;

/**
 * In all cases, garbage prefixing NVRA on server is ignored.
 *
 * Supported cases for NVRA:
 *
 * where the paths may be absolute or relative or nothing
 * <ul>
 * <li>scp any/file user@host:any/path/nvra</li>
 * <li>scp any/path/NVRA user@host:any/path/</li>
 * <li>scp any/path/NVRA user@host:any/path/DifferentNVRA (renaming on
 * server)</li>
 * </ul>
 * similar, download
 * <ul>
 * <li>scp user@host:any/path/nvra any/dir/ </li>
 * <li>scp user@host:any/path/DifferentNVRA any/path/NVRA (renaming on
 * client)</li>
 * </ul>
 * Similarly, multiple uploads/downloads works
 * <ul>
 * <li>scp any/path/NVRA1 any/path/NVRA2 user@host</li>
 * <li>scp user@host:NVRA1 user@host:NVRA2 dir</li>
 * </ul>
 *
 * Supported cases for data (files shared by all builds, sources and
 * architectures):
 *
 * where the paths may be absolute or relative or nothing
 * <ul>
 * <li>scp any/file user@host:any/path/nvra/data</li>
 * <li>scp any/file user@host:any/path/nvra/data/newName (renaming on
 * server)</li>
 * </ul>
 * similar, download
 * <ul>
 * <li>scp user@host:nvra/data/fileName any/dir/ </li>
 * <li>scp user@host:nvra/data/fileName any/dir/newName(renaming on client)</li>
 * </ul>
 * Similarly, multiple uploads/downloads works
 * <ul>
 * <li>scp dataFile1 dataFile2 user@host:NVRA/data</li>
 * <li>scp user@host:NVRA1/data/fileX user@host:NVRA2/data/fileY dir</li>
 * </ul>
 * Supported cases for logs - logs of all sources and builds are kept arch by
 * arch For comaptibility reasons, you can use both "nvra/log/filename" and
 * "nvra/data/log/filename"
 *
 * where the paths may be absolute or relative or nothing
 * <ul>
 * <li>scp any/file user@host:any/path/nvra/log</li>
 * <li>scp any/file user@host:any/path/nvra/log/newName (renaming on
 * server)</li>
 * </ul>
 * similar, download
 * <ul>
 * <li>scp user@host:nvra/log/fileName any/dir/ </li>
 * <li>scp user@host:nvra/log/fileName any/dir/newName(renaming on client)</li>
 * </ul>
 * Similarly, multiple uploads/downloads works
 * <ul>
 * <li>scp dataFile1 dataFile2 user@host:NVRA/log</li>
 * <li>scp user@host:NVRA1/log/fileX user@host:NVRA2/log/fileY dir</li>
 * </ul>
 * koji-like FS loks like this:
 * <ul>
 * <li>Name/Version/Release/arch/binaryForThatArch</li>
 * <li>Name/Version/Release/src/sources</li>
 * <li>Name/Version/Release/data/ for generic data files about whole n/v/r</li>
 * <li>Name/Version/Release/data/logs/arch for variosu metadata about given
 * build (or srrc)</li>
 * </ul>
 * You can uplaod any subpath into Name/Version/Release/arch/, but it is not
 * recommended (TODO, remeove this feature?). Also be aware, that
 * <ul>
 * <li>scp any/fileName user@host:any/path/nvra/any/path
 * </ul>
 * will lead to:
 * <ul>
 * <li>Name/Version/Release/arch/any/path</li>
 * </ul>
 * where path is new name for fileName unlike directory
 * Name/Version/Release/arch/any/path already existed, which is unlikely
 *
 * The recursive uplaods of binaries, logs and data should work as expected:
 * <ul>
 * <li>heaving dir with nvra1 and nvra2 (even in subdirs) then scp -r dir
 * user@host: will uplaod two NVRAs to their correct places</li>
 * <li>heaving dir with logfile1 and logfile2 (even in subdirs)then scp -r dir
 * user@host:NVRA/logs will uplaod two the logs for given NVRA to correct
 * place</li>
 * <li>heaving dir with data1 and data2 (even in subdirs) then scp -r dir
 * user@host:NVRA/data will uplaod two the files for given NVRA to correct
 * palce</li>
 * </ul>
 * Mixed conntent is not allowe (eg data+logs together). Also custom paths are
 * not supported in recursive upload
 * <p>
 * Scp recursive download si now a bit broken:
 * <ul>
 * <li>`scp -r user@host:nvra dir` will scp the nvra tarball, ignoring any
 * -r</li>
 * <li>multiple tarballs for one nvra are unlikely to be supported</li>
 * <li>`scp -r user@host:nvra/logs dir` will correctly scp all logs for given
 * nvra. Directories are honored in `dir`</li>
 * <li>`scp -r user@host:nvra/data/logs dir` is currently broken. Should be
 * fixed</li>
 * <li>`scp -r user@host:nvra/data dir` is currently broken. Should be fixed.
 * SHoudl gave you all data for given NVR and all logs of all A for given
 * NVR(A)</li>
 * </ul>
 *
 *
 * @author jvanek
 */
public class SshApiService {

    private static final String AUTHORIZED_KEYS = "authorized_keys";
    private static final String ID_RSA_PUB = "id_rsa.pub";
    private static final String terrible_env_var = "FAKE_KOJI_ALTERNATE_ID_RSA_PUB_OR_AUTHORISED_KEYS";

    private File dbRoot;
    private int port;
    private String[] keys;

    private SshServer sshServer;

    public SshApiService(File dbRoot, int port) {
        this.dbRoot = dbRoot;
        this.port = port;
    }

    public SshApiService(File dbRoot, int port, String... keys) {
        this.dbRoot = dbRoot;
        this.port = port;
        this.keys = keys;
    }

    public void start() throws IOException, GeneralSecurityException {
        if (sshServer == null) {
            if (keys == null) {
                sshServer = setup(port, dbRoot);
            } else {
                sshServer = setup(port, dbRoot, keys);
            }
        }
        sshServer.start();
    }

    public int getPort() {
        return port;
    }
    
    

    public void stop() throws IOException {
        if (sshServer != null) {
            sshServer.stop(true);
        }
    }

    public SshServer setup(int port, final File dbRoot) throws IOException, GeneralSecurityException {
        String presetKeys = System.getenv(terrible_env_var);
        String[] keys;
        if (presetKeys == null || presetKeys.isEmpty()) {
            ServerLogger.log("ignoring " + terrible_env_var + " with space separated user=/path/to/pubOrauthorisedKeys and using defaults");
            keys = getTesterKeystore().split("\\s+");
        } else {
            ServerLogger.log("using " + terrible_env_var);
            keys = presetKeys.trim().split("\\s+");
        }
        return setup(port, dbRoot, keys);
    }

    public SshServer setup(int port, final File dbRoot, String... keys) throws IOException, GeneralSecurityException {
        this.dbRoot = dbRoot;
        if (!dbRoot.exists()) {
            throw new RuntimeException(dbRoot + " dont exists");
        }
        if (!dbRoot.isDirectory()) {
            throw new RuntimeException(dbRoot + " must be dir!");
        }
        SshServer sshd = SshServer.setUpDefaultServer();
        sshd.setPort(port);
        sshd.setPublickeyAuthenticator(new UserKeySetPublickeyAuthenticator(readAuthorizedKeys(keys)));
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File(System.getProperty("user.home"), ".fake-koji.hostkey")));

        //this is quite dummy impl, simply  giving stream from correct location to write file to new, our location
        ScpCommandFactory sf = new ScpCommandFactory() {
        };

        sf.setScpFileOpener(new ScpFileOpener() {

            @Override
            public Path resolveIncomingFilePath(Path localPath, String name, boolean preserve, Set<PosixFilePermission> permissions, ScpTimestamp time) throws IOException {
                return localPath;
            }

            @Override
            public Path resolveIncomingReceiveLocation(Path path, boolean recursive, boolean shouldBeDir, boolean preserve) throws IOException {
                return path;
            }

            @Override
            public Path resolveOutgoingFilePath(Path localPath, LinkOption... options) throws IOException {
                try {
                    return new File(dbRoot, deductPathName(localPath.toString())).toPath();
                } catch (Exception ex) {
                    return localPath;
                }

            }

            @Override
            public ScpSourceStreamResolver createScpSourceStreamResolver(Path path) throws IOException {
                return new ScpSourceStreamResolverImpl(path);
            }

            @Override
            public ScpTargetStreamResolver createScpTargetStreamResolver(Path path) throws IOException {
                return new ScpTargetStreamResolver() {
                    @Override
                    public OutputStream resolveTargetStream(Session sn, String name, long l, Set<PosixFilePermission> set, OpenOption... oos) throws IOException {
                        Path lPath = mergeNameIntoPathOrNot(path, name);
                        return sf.getScpFileOpener().openWrite(sn, lPath, oos);
                    }

                    @Override
                    public Path getEventListenerFilePath() {
                        return null;
                    }

                    @Override
                    public void postProcessReceivedData(String string, boolean bln, Set<PosixFilePermission> set, ScpTimestamp st) throws IOException {
                        //todo something here?
                    }

                    private Path mergeNameIntoPathOrNot(Path path, String name) {
                        if (name == null || name.isEmpty() || path.endsWith(name)) {
                            return path;
                        }
                        if (path.endsWith("logs") || path.endsWith("data")) {
                            return path.resolve(name);
                        }
                        String isParsableName = null;
                        try {
                            isParsableName = deductPathName(name);
                        } catch (Exception ex) {
                            //is not
                        }
                        File f;
                        try {
                            //this is hook for sending no path with NVRA
                            f = new File(dbRoot, deductPathName(path.toString()));
                        } catch (Exception ex) {
                            try {
                                f = new File(dbRoot, deductPathName(path.resolve(name).toString()));
                                return f.toPath();
                            } catch (Exception exx) {
                                return path;
                            }
                        }
                        if (f.exists() && f.isDirectory()) {
                            return path.resolve(name);
                        }
                        return path;
                    }
                };
            }

            class ScpSourceStreamResolverImpl implements ScpSourceStreamResolver {

                private RealPaths realPath;
                private Path origPath;

                private ScpSourceStreamResolverImpl(Path path) throws SshException {
                    origPath = path;
                    try {
                        realPath = createRealPaths(path);
                    } catch (Exception ex) {
                        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                            //already resolved?
                            realPath = new RealPaths(path.toString(), path.toFile());
                            origPath = null;
                        } else {
                            throw ex;
                        }
                    }
                    if (!realPath.fullPath.exists()) {
                        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                            realPath = new RealPaths(path.toString(), path.toFile());
                        } else {
                            throw new SshException(realPath.fullPath + " dont exisits!");
                        }
                    }
                }

                @Override
                public String getFileName() throws IOException {
                    return realPath.fullPath.getName();
                }

                @Override
                public Path getEventListenerFilePath() {
                    return null;
                }

                @Override
                public Collection<PosixFilePermission> getPermissions() throws IOException {
                    return Files.getPosixFilePermissions(realPath.fullPath.toPath(), LinkOption.NOFOLLOW_LINKS);
                }

                @Override
                public ScpTimestamp getTimestamp() throws IOException {
                    return new ScpTimestamp(realPath.fullPath.lastModified(), realPath.fullPath.lastModified());
                }

                @Override
                public long getSize() throws IOException {
                    return realPath.fullPath.length();
                }

                @Override
                public InputStream resolveSourceStream(Session sn, OpenOption... oos) throws IOException {
                    if (origPath == null) {
                        return new FileInputStream(realPath.fullPath);
                    } else {
                        //there is need to send original path, as the ScpFileOpenr may be called on its own and so reparse the name
                        return sf.getScpFileOpener().openRead(sn, origPath, oos);
                    }
                }
            }

            @Override
            public InputStream openRead(Session session, Path file, OpenOption... options) throws IOException {
                ServerLogger.log("Accepting downlaod of " + file);
                RealPaths paths = createRealPaths(file);
                if (!paths.fullPath.exists()) {
                    if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
                        return new FileInputStream(file.toFile());
                    }
                    String ss = paths.fullPath.toString() + " dont exists. ";
                    ServerLogger.log(ss);
                    throw new SshException(ss);
                }
                return new FileInputStream(paths.fullPath);
            }

            @Override
            public OutputStream openWrite(Session session, Path file, OpenOption... options) throws IOException {
                ServerLogger.log("Accepting upload to " + file);
                RealPaths paths = createRealPaths(file);
                if (paths.fullPath.exists()) {
                    String ss = paths.fullPath.toString() + " already exists. Overwrite is disabled right now";
                    ServerLogger.log(ss);
                    throw new SshException(ss);
                }
                File parent = paths.fullPath.getParentFile();
                ServerLogger.log("ensuring " + parent.getAbsolutePath());
                createCorrectlyOwnedDirectoryTree(parent, session.getUsername());
                boolean blnCreated = paths.fullPath.createNewFile();
                if (!blnCreated) {
                    String ss = paths.fullPath.toString() + " failed to create, exiting sooner ratehr then later";
                    ServerLogger.log(ss);
                    throw new SshException(ss);
                }
                setOwner(paths.fullPath.toPath(), session.getUsername());
                return new FileOutputStream(paths.fullPath);
            }

            private void createCorrectlyOwnedDirectoryTree(File dirr, String username) throws IOException {
                List<Path> dirs = splitDirs(dirr.toPath());
                for (Path dir : dirs) {
                    if (!dir.toFile().exists()) {
                        //we try to change owner only on dirs which we creates
                        boolean r = dir.toFile().mkdir();
                        if (r == true) {
                            setOwner(dir, username);
                        } else {
                            ServerLogger.log("Faield to create directory " + dir.toString());
                        }
                    }
                }
            }

            private void setOwner(Path path, String user) throws IOException {
                //setOwner fails for non root
                if ("root".equals(System.getProperty("user.name"))) {
                    UserPrincipalLookupService lookupService = FileSystems.getDefault().getUserPrincipalLookupService();
                    UserPrincipal userPrincipal = lookupService.lookupPrincipalByName(user);
                    Files.setOwner(path, userPrincipal);
                } else {
                    //ServerLogger.log("Not changing ownership from " + System.getProperty("user.name") + " to " + user + " for " + path + ". Upload service is not running as root.");
                }
            }

            private List<Path> splitDirs(Path file) {
                List<Path> result = new ArrayList();
                while (true) {
                    result.add(file);
                    file = file.getParent();
                    if (file == null) {
                        break;
                    }
                }
                Collections.reverse(result);
                return result;
            }

        });

        sshd.setCommandFactory(sf);
        return sshd;
    }

    private static Map<String, Collection<? extends PublicKey>> readAuthorizedKeys(String... setKeys) throws IOException, GeneralSecurityException {
        Map<String, Collection<? extends PublicKey>> userKeysMap = new HashMap<>();
        for (String userKeys : setKeys) {
            String user = userKeys.split("=")[0];
            String file = userKeys.split("=")[1];
            List<PublicKey> usersKeys = new ArrayList<>();
            File f = new File(file);
            if (f.exists() && f.canRead()) {
                ServerLogger.log("For " + user + " adding from " + file);
                for (AuthorizedKeyEntry ake : AuthorizedKeyEntry.readAuthorizedKeys(f)) {
                    PublicKey publicKey = ake.resolvePublicKey(PublicKeyEntryResolver.IGNORING);
                    if (publicKey != null) {
                        usersKeys.add(publicKey);
                    }
                }
                userKeysMap.put(user, usersKeys);
            } else {
                ServerLogger.log("inaccessible  from " + file);
            }
        }
        return userKeysMap;
    }

    private static String getTesterKeystore() {
        String defaultSshdKeys1 = getDefaultKeyStores();
        String defaultSshdKeys2 = getDefaultKeyStores("tester");
        String secondarySshKeys = getSecodaryKeyStores("tester");
        return defaultSshdKeys1 + " " + defaultSshdKeys2 + "" + secondarySshKeys;
    }

    private static String getDefaultKeyStores() {
        return System.getProperty("user.name") + "=" + defaultSshDir() + AUTHORIZED_KEYS + " " + System.getProperty("user.name") + "=" + defaultSshDir() + ID_RSA_PUB;
    }

    private static String getDefaultKeyStores(String user) {
        return user + "=" + defaultSshDir(user) + AUTHORIZED_KEYS + " " + user + "=" + defaultSshDir(user) + ID_RSA_PUB;
    }

    private static String getSecodaryKeyStores(String user) {
        return user + "=" + defaultSshDir() + "/" + user + "/" + AUTHORIZED_KEYS + " " + user + "=" + defaultSshDir() + "/" + user + "/" + ID_RSA_PUB;
    }

    private static String defaultSshDir() {
        return System.getProperty("user.home") + "/.ssh/";
    }

    private static String defaultSshDir(String user) {
        return "/home/" + user + "/.ssh/";
    }

    public static class UserKeySetPublickeyAuthenticator implements PublickeyAuthenticator {

        private final Map<String, Collection<? extends PublicKey>> userToKeySet;

        public UserKeySetPublickeyAuthenticator(Map<String, Collection<? extends PublicKey>> userToKeySet) {
            this.userToKeySet = userToKeySet;
        }

        @Override
        public boolean authenticate(String username, PublicKey key, ServerSession session) {
            return KeyUtils.findMatchingKey(key, userToKeySet.getOrDefault(username, Collections.emptyList())) != null;
        }

    }

    public static String deductPathName(String name) {
        //blah/blah/nvra/logs/logName
        //blah/blah/nvra/data/fileName
        //blah/blah/nvra
        //blah/blah/?opts?/nvra
        //blah/blah/nvra/?opts?
        //opts are currnelty not implemented and hopefully never will
        String[] parts = name.split("[/]+");
        if (name.contains("/logs") || name.contains("/data")) {
            String fileName;
            String type;
            String nvraName;
            ///data/logs are considered same as  /logs and vice versa
            if (name.contains("/data/logs")) {
                fileName = parts[parts.length - 1];
                type = parts[parts.length - 2];
                nvraName = parts[parts.length - 4];
            } else {
                fileName = parts[parts.length - 1];
                type = parts[parts.length - 2];
                nvraName = parts[parts.length - 3];
            }
            if (fileName.equals("logs") || fileName.equals("data")) {
                //recursive download?
                nvraName = type;
                fileName = "";
            }
            NVRA nvra = new NVRA(nvraName);
            if (name.contains("/logs")) {
                return nvra.getBasePath() + "/data/logs/" + nvra.arch + "/" + fileName;
            } else if (name.contains("data/")) {
                return nvra.getBasePath() + "/data/" + fileName;
            } else {
                throw new NvraParsingException(name);
            }

        } else {
            int i = findTopMostParsableNvra(parts);
            if (i < 0) {
                i = parts.length - 1;
            }
            String nvraName = parts[i];
            NVRA nvra = new NVRA(nvraName);
            if (i == parts.length - 1) {
                return nvra.getArchedPath() + tail(i, parts);
            } else {
                return nvra.getArchedPath() + tail(i + 1, parts);
            }
        }

    }

    private static int findTopMostParsableNvra(String[] parts) {
        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i];
            try {
                NVRA nvra = new NVRA(part);
                return i;
            } catch (Exception ex) {
            }
        }
        return -1;
    }

    private static String tail(int index, String[] parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = index; i < parts.length; i++) {
            String part = parts[i];
            sb.append("/").append(part);
        }
        return sb.toString();
    }

    private static class NvraParsingException extends RuntimeException {

        private NvraParsingException(String original, Exception ex) {
            super(getMessage(original), ex);
        }

        public static String getMessage(String s) {
            return "cant parse `" + s + "` to path. Expected NVRA.suffix or NVRA.suffix/some/sub/path (eg /data/filename or /data/logs/filename or /logs/filename)";
        }

        private NvraParsingException(String original) {
            super(getMessage(original));
        }

    }

    private static class NVRA {

        private final String releaseArchSuffix;
        private final String version;
        private final String product;
        private final String suffix;
        private final String arch;
        private final String release;

        public NVRA(String name) {
            try {
                String[] split = name.split("-");
                //split.length - 1 == release.arch.suffix
                //split.length - 2 == version
                //rest, down to 0, is product 
                releaseArchSuffix = split[split.length - 1];
                version = split[split.length - 2];
                product = name.replace("-" + version + "-" + releaseArchSuffix, "");
                String[] ras = releaseArchSuffix.split("\\.");
                suffix = ras[ras.length - 1];
                arch = ras[ras.length - 2];
                release = releaseArchSuffix.replace("." + arch + "." + suffix, "");
            } catch (Exception ex) {
                throw new NvraParsingException(name, ex);
            }
        }

        public String getArchedPath() {
            return getBasePath() + "/" + arch;
        }

        public String getBasePath() {
            return product + "/" + version + "/" + release.replace("static", "upstream");
        }
    }

    private RealPaths createRealPaths(Path file) throws SshException {
        String newPath = null;
        try {
            newPath = deductPathName(file.toString());
            ServerLogger.log("Filename is " + newPath);
        } catch (Exception e) {
            throw new SshException(e);
        }
        File fullPath = new File(dbRoot.getAbsolutePath() + "/" + newPath);
        return new RealPaths(newPath, fullPath);
    }

    private static class RealPaths {

        private final String newPath;
        private final File fullPath;

        @Override
        public String toString() {
            return newPath + " (" + fullPath + ")";
        }

        private RealPaths(String newPath, File fullPath) {
            this.newPath = newPath;
            this.fullPath = fullPath;
        }
    }

}

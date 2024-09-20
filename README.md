# Fake Koji SCM

### Table of Contents
* [Fake Koji](#fake-koji)
    * [Naming Convention](#naming-convention)
    * [SCP Upload](#scp-upload)
    * [XML-RPC API](#xml-rpc-api)
    * [Curl API](#curl-api)
    * [HTTP File Listing](#http-file-listing)
    * [HTTP View](#http-view)
* [Jenkins SCM Koji Plugin](#jenkins-scm-koji-plugin)
* [Future goals](#future-goals)

## Fake Koji
Fake Koji serves as a storage for builds along with logs and source snapshots.

### Naming Convention

Fake Koji uses Koji's NVRA (name, version, release, architecture):
* `name` represents java product (java-10-openjdk, java-11-openjdk)
* `version` represents tag (jdk.10.46, jdk.11.23)
* `release` represents:
  * repository i.e. (shenandoah, ember)
  * number of changesets since last tag
  * keywords (static, slowdebug, fastdebug, zero, openj9)
* `architecture` can be:
  * src containing source snapshot
  * real platform (x86_64, i686, aarch64, win10_x64, rhel7_aarch64, fedora28_x64, centos7_i386)
* `data` stores build log for each architecture and pull/incoming logs for src

### SCP Upload
###### Default port: 9822
#### Supported cases for NVRA

##### Upload
`scp ./dir/file user@host:nvra`

`scp ./dir/nvra user@host`

`scp ./dir/nvra user@host:/any_path/differentNvra`

`scp .dir/nvra user@host:/any_path/nvra`

The uploaded file will be stored in `user@host:/path_to_builds/n/v/r/a/`.
Note that in the last two cases the `any_path`, where the NVRA should be stored, is ignored.

##### Download:
`scp user@host:nvra ./dir/`

`scp user@host:nvra ./dir/differentNvra`

The downloaded file: `./dir/nvra`.

##### Multiple upload and download
Works similarly:

`scp ./dir/nvra1 ./dir/nvra2 user@host`

`scp user@host:nvra1 user@host:nvra2 ./dir`

#### Supported cases for logs

##### Upload
`scp ./dir/logFile user@host:nvra/log`

`scp ./dir/logFile user@host:nvra/log/newFile`

The `logFile` will be stored in `user@host:/path_to_builds/n/v/r/data/logs/a/`

##### Download
`scp user@host:nvra/log/logFile ./dir`

`scp user@host:nvra/log/logFile ./dir/newLogFile`

The downloaded file: `./dir/logFile`

##### Multiple upload and download
Works the same as single upload/download:

`scp logFile1 logFile2 user@host:nvra/log`

`scp user@host:nvra1/log/logFile1 user@host:nvra2/log/logFile2 ./dir`

##### Recursive upload

Having `nvra1` and `nvra2` in `./dir`, then

`scp -r ./dir user@host`

will upload nvras to `user@host:/path_to_builds/n1/v1/r1/a1` and `user@host:/path_to_builds/n2/v2/r2/a2`, respectively.

Having `logFile1` and `logFile2` in `./dir`, then

`scp -r ./dir user@host:nvra/logs`

will upload both log files to `user@host:/path_to_builds/n/v/r/data/logs/a/`.

Having `data1` and `data2` in `./dir`, then

`scp -r ./dir user@host:nvra/data`

will upload both files to `user@host:/path_to_builds/n/v/r/data/`

##### Recursive download
NVRA:

Recursive download of nvra works same as normal download. Multiple tarballs for one nvra is unlikely to be supported.

Logs:

`scp -r user@host:nvra/logs ./dir`

will download all logs for given nvra.

`scp -r user@host:nvra/data/logs ./dir`

and

`scp -r user@host:nvra/data ./dir`

are currently broken.


### XML-RPC API
###### Default port: 9848
Part of Fake Koji is xml-rpc server compatible with Koji and Brew. You can get build information using these methods:
* getPackageId
  * parameter: package name
  * returns the package id if given package
* ListBuilds
  * parameter: package id
  * returns all builds of given package
* listTags
  * parameter: build id
  * returns tags of given build
  * tags tell us whether build is done on a cetain architecture or not
* listRPMs
  * parameter: build id, architectures
  * returns all build's rpms of given architectures
* listArchives
  * parameter: build id, architectures
  * archives represent .tar.gz, .zip and .msi files
  * returns build's archives of given architectures

### Curl API
You can get various information about Fake Koji's configuration and storage using its curl api.

##### Basic usage:

`curl host/get?option:argument`

Alternatively you can use url bar in your browser.

##### Options:
* `allProducts`
  * lists all available products
* `allProjects`
  * lists all available projects
* `dport`
  * download port
* `expectedArchesOfNvr`
  * returns list of architectures the nvra is supposed to be built on
  * argument: nvra (`java-10-openjdk-jdk.10.46-0.static.x86_64`)
* `expectedArchesOfProject`
  * returns list of architectures the project is supposed to be built on
  * argument: project (`java-10-openjdk-updates`)
* `productOfNvra`
  * returns product of given nvra
  * argument: nvra
* `productOfProject`
  * returns product of given project
  * argument: project
* `projectOfNvra`
  * returns project of given nvra
  * argument: nvra
* `projectsOfProduct`
  * list of projects of given product
  * argument: product (`java-10-openjdk`)
* `repos`
  * path to folder containing repositories
* `root`
  * path to folder containing builds
* `uport`
  * scp upload port
* `viewlport`
  * view port
* `xport`
  * XML-RPC port

### HTTP File Listing
###### Default port: 9849
Here you can browse Fake Koji's folder structure. There are three sections with same content but different sorting. The first one uses version sorting(last version on top), the second uses latest modified sorting(the latest modified file on top) and the last one uses latest modified directory content.
#### Folder structure
<pre>
└── name
    └── version
        └── release
            ├── arch1
            │   └── name-version-release-arch1.tar.gz
            ├── arch2
            │   └── name-version-release-arch2.tar.gz
            ├── arch3
            │   └── name-version-release-arch3.tar.gz
            ├── data
            │   └── logs
            │       ├── arch1
            │       ├── arch2
            │       ├── arch3
            │       └── src
            └── src
                └── name-version-release-src.tar.gz
</pre>

![fakekoji-file-listing-01](https://user-images.githubusercontent.com/31389543/43505699-0be4a604-9568-11e8-8c0f-7cba740e513f.png)

![fakekoji-file-listing-02](https://user-images.githubusercontent.com/31389543/43505704-1079a46c-9568-11e8-8ef5-46d57e584091.png)

![fakekoji-file-listing-03](https://user-images.githubusercontent.com/31389543/43505706-12474344-9568-11e8-9817-ffcfb4bfc6e8.png)

![fakekoji-file-listing-04](https://user-images.githubusercontent.com/31389543/43505710-14461d96-9568-11e8-9bf5-bfd012ce5c3d.png)

### HTTP View
###### Default port: 80
This is the frontend of fake Koji. Here you can see the latest successful builds of every project. Projects are divided by their product. To see all builds of a project by clicking on project's details.

![fake-koji-preview](https://user-images.githubusercontent.com/31389543/43505713-167730fa-9568-11e8-89de-39a43297c136.png)

![fake-koji-preview-details](https://user-images.githubusercontent.com/31389543/43505715-198779ee-9568-11e8-95ac-cc9e87626d51.png)

## Jenkins SCM Koji Plugin
Next to Fake Koji there is Jenkins plugin, which ensures cooperation between Jenkins and Fake Koji. Based on configuration, plugin provides builds. `-Dhudson.remoting.ClassFilter=hudson.plugins.scm.koji.model.Build,hudson.plugins.scm.koji.model.RPM,hudson.plugins.scm.koji.model.BuildProvider` may be still necessary to add to jenkins start up.
#### Configuration
* `Koji top URLs`
  * URL of Kojihub, Brewhub or Fake Koji
  * this is the URL of xml-rpc server, from which plugin gets builds' information
  * can contain more than URLs separated by space, first URL found first served
* `Koji download URLs`
  * RPMs or archives get downloaded from these URLs
  * can contain multiple URLs separated by space, first URL found first served
* `Package name`
  * package name to watch
  * can contain multiple names separated by space
* `Package arch`
  * only builds for specified arch will be used
  * can be empty
  * can contain multiple archs separated by coma
* `Package tag`
  * only builds containing specified tag
  * may be empty to match all the builds
  * supports glob pattern
* `Excluding NVRs`
  * glob patterns for NVRs not to download.
  * example: `{*debug*, *src*}`
* `Download directory`
  * name of directory inside Jenkins' workspace to download RPMs/archives
  * if empty, the RPMs/archives will be downloaded directly into workspace
* `Max previous builds`
  * max number of previous builds to check, in case there's more of them
* `Clean download directory`
  * removes all the content of the download directory before the download
* `Create subdirectory for each NVR`
  * creates subdirectiory under the download directory where the name of subdirectory is going to be the name of the NVR of the RPM package

##### Configuration examples

![koji-plugin-config-01](https://user-images.githubusercontent.com/31389543/43509489-1cb3f268-9573-11e8-925e-634b4100b5fb.png)

![koji-plugin-config-02](https://user-images.githubusercontent.com/31389543/43509492-1e6c8f0c-9573-11e8-976f-86c87db1f4e4.png)

### Results package

After Jenkins jobs is finished, the plugin generates a page with results information. This page shows what specific build was used(name, version, release, NVR, tags), what RPMs or archives were downloaded, links and hash sums to this build can be also found here. If available, sources of build are included.
![jenkinks-plugin-result-page](https://user-images.githubusercontent.com/31389543/43505723-1b2f39ee-9568-11e8-829c-ee03f6677a62.png)

## Future goals

* Project/Repository management
* Implementation of HTTPS
* JSON API
* New frontend (Angular app)

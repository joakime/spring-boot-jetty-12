def buildScan = session.lookup('com.gradle.maven.extension.api.scan.BuildScanApi')

buildScan.executeOnce('custom-data') { api ->
    tagOs(api)
    tagIde(api)
    addGitMetadata(api)
}

static void tagOs(def api) {
    api.tag System.getProperty('os.name')
}

static void tagIde(def api) {
    if (System.getProperty('idea.version')) {
        api.tag 'IntelliJ IDEA'
    } else if (System.getProperty('eclipse.buildId')) {
        api.tag 'Eclipse'
    }
}

static void addGitMetadata(def api) {
    api.background { bck ->
        if (!isGitInstalled()) {
            return
        }
        def gitCommitId = execAndGetStdout('git', 'rev-parse', '--short=8', '--verify', 'HEAD')
        def gitBranchName = execAndGetStdout('git', 'rev-parse', '--abbrev-ref', 'HEAD')
        def gitStatus = execAndGetStdout('git', 'status', '--porcelain')

        if (gitCommitId) {
            def commitIdLabel = 'Git commit id'
            bck.value commitIdLabel, gitCommitId
            addCustomValueSearchLink bck, 'Git commit id build scans', [(commitIdLabel): gitCommitId]
            def originUrl = execAndGetStdout('git', 'config', '--get', 'remote.origin.url')
            if (originUrl.contains('github.com')) { // only for GitHub
                def repoPath = (originUrl =~ /(.*)github\.com[\/|:](.*)/)[0][2]
                if (repoPath.endsWith('.git')) {
                    repoPath = repoPath.substring(0, repoPath.length() - 4)
                }
                bck.link 'Github Source', "https://github.com/$repoPath/tree/" + gitCommitId
            }
        }
        if (gitBranchName) {
            bck.tag gitBranchName
            bck.value 'Git branch', gitBranchName
        }
        if (gitStatus) {
            bck.tag 'Dirty'
            bck.value 'Git status', gitStatus
        }
    }
}

static String execAndGetStdout(String... args) {
    def exec = args.toList().execute()
    exec.waitFor()
    trimAtEnd(exec.text)
}

static void addCustomValueSearchLink(def api, String title, Map<String, String> search) {
    if (api.server) {
        api.link title, customValueSearchUrl(api, search)
    }
}

static String customValueSearchUrl(def api, Map<String, String> search) {
    def query = search.collect { name, value ->
        "search.names=${encodeURL(name)}&search.values=${encodeURL(value)}"
    }.join('&')
    "${appendIfMissing(api.server, '/')}scans?$query"
}

static String encodeURL(String url) {
    URLEncoder.encode(url, 'UTF-8')
}

static boolean isGitInstalled() {
    try {
        'git --version'.execute().waitFor() == 0
    } catch (IOException ignored) {
        false
    }
}

static String appendIfMissing(String str, String suffix) {
    str.endsWith(suffix) ? str : str + suffix
}

static String trimAtEnd(String str) {
    ('x' + str).trim().substring(1)
}
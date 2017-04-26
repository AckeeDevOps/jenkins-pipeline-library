def call(String source, String dest, String excludeDir) {
  sh("gsutil -m rsync -R -x '${excludeDir}' ${source} ${dest}")
}

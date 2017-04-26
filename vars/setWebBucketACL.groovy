def call(String bucketURL) {
  sh("gsutil -m acl ch -R -u AllUsers:R ${bucketURL}")
}

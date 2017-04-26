def call(String bucketURL) {
  // serve all requests with index.html
  sh("gsutil -m web set -m index.html -e index.html ${bucketURL}")
}

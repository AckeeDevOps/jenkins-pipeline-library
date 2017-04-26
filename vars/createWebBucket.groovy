def call(String bucketURL) {
  // don't fail if bucket exists
  sh("gsutil mb -c regional -l europe-west1 $bucketURL 2>&1 | grep 'ServiceException: 409' && echo 'Bucket exists'")
}

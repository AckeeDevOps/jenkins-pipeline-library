def call(String name) {
 def revproxyURL='c.storage.googleapis.com'
 createCNAMERecord(name, revproxyURL)
}

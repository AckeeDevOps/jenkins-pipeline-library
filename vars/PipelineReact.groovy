// vars/buildReact.groovy
def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    // now build, based on the configuration provided
    node() {
        
        
    def workspace = pwd()
    def appDir = '/usr/src/app'
    def nodeEnv = "${config.nodeEnv} -v ${workspace}:${appDir} -w ${appDir}"
    // TODO: verify if not null and if the value is correct
    def nodeImage = config.nodeImage
    def buildCommand = config.buildCommand
    def baseURL = config.baseURL
    def bucketURL = config.bucketURL
    def buildDir = config.buildDir
    def slackChannel = config.slackChannel
    def excludeDir = config.excludeDir
    
    try {
     
      checkoutScm()
      
      stage('Build') {
        docker.image(nodeImage).inside(nodeEnv) {
            sh buildCommand
        }
      }
      stage('Set Bucket') {
        createDNSforBucket(baseURL)
        createWebBucket(bucketURL)
      }
       
      stage('Deploy to Bucket'){
        deployToBucket(buildDir, bucketURL, excludeDir)
      }
      
      stage('Set bucket ACL'){
        setWebBucketACL(bucketURL)
      }
      
      stage('Set bucket index and error pages'){
        setReactBucketWebserver(bucketURL)
      } 
      
      } catch (err) {
          currentBuild.result = "FAILURE"
          slackSend message: "Build #$env.BUILD_NUMBER ${baseURL} *failed* (<$env.BUILD_URL|open>)", channel : slackChannel, color: "bad"
          throw err
      }
      slackSend message: "Build #$env.BUILD_NUMBER ${baseURL} *completed* started by $env.GIT_AUTHOR_NAME (<$env.BUILD_URL|open>)", channel : slackChannel, color: "good"
    }
}

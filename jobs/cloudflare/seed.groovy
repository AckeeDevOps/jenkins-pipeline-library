String folderName = 'cloudflare'
String scriptPath = 'jobs'

folder(folderName) {
    description 'CloudFlare - automatically created cloudflare jobs.'
}

domains = ["example.com", "ackee.cz", "ackee.de"]

domains.each {
    
    def name = it
   
    pipelineJob("$folderName/${name}-dev-mode") {
        
        concurrentBuild(false)
        
        definition {
            cps {
                script("cfEnableDevMode('${name}')")
                sandbox()
            }
        }
    }
    
    pipelineJob("$folderName/${name}-purge-cache") {
        
        concurrentBuild(false)
        
        definition {
            cps {
                script("cfPurgeAll('${name}')")
                sandbox()
            }
        }
    } 
    
}



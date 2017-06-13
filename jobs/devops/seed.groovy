String folderName = 'devops'
String agentLabel = 'monitoring'
String scriptPath = "jobs/${folderName}"

folder(folderName) {
    description 'DevOps - automatically created jobs.'
}

pipelineJob("$folderName/checkBackups") {
    
    label(agentLabel)
    
    concurrentBuild(false)
    
    definition {
        cps {
            script("node('$agentLabel') { checkBackups() }")
            sandbox()
        }
    }
    
    triggers {
        cron('0 12 * * *')
    }
}

pipelineJob("$folderName/checkBucketsDiskUsage") {
    
    label(agentLabel)
    
    concurrentBuild(false)
    
    definition {
        cps {
            script(readFileFromWorkspace("${scriptPath}/checkBucketsDiskUsage.groovy"))
            sandbox()
        }
    }
    
    triggers {
        cron('@daily')
    }
}

pipelineJob("$folderName/checkGCESnapshots") {
    
    label(agentLabel)
    
    concurrentBuild(false)
    
    definition {
        cps {
            script(readFileFromWorkspace("${scriptPath}/checkGCESnapshots.groovy"))
            sandbox()
        }
    }
    
    triggers {
        cron('@daily')
    }
}

pipelineJob("$folderName/checkLoadbalancers") {
    
    label(agentLabel)
    
    concurrentBuild(false)
    
    definition {
        cps {
            script("node('$agentLabel') { checkBackups() }")
            sandbox()
        }
    }
    
    triggers {
        cron('@daily')
    }
}

pipelineJob("$folderName/checkNonattachedDisks") {
    
    label(agentLabel)
    
    concurrentBuild(false)
    
    definition {
        cps {
            script(readFileFromWorkspace("${scriptPath}/checkNonattachedDisks.groovy"))
            sandbox()
        }
    }
    
    triggers {
        cron('@daily')
    }
}

// no triggers! only manually
pipelineJob("$folderName/checkUrls") {
    
    label(agentLabel)
    
    concurrentBuild(false)
    
    definition {
        cps {
            script(readFileFromWorkspace("${scriptPath}/checkUrls.groovy"))
            sandbox()
        }
    }
}


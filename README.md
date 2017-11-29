# jenkins-pipeline-library
this is our jenkins pipeline shared library for CI/CD to our Kubernetes cluster hosted on GKE

## These pipelines use these clients/APIs 
* Google Cloud 
* Kubernetes
* Docker
* Cloudflare
* Jenkins
* Slack

## Currently this repo supports
* ReactJS @ Google Storage Buckets - check `templates/react/Jenkinsfile` and `var/PipelineReact`
* NodeJS @ Google Container Engine - check `templates/nodejs/Jenkinsfile` and `var/PipelineNodejs`
* Middleman @ Google Storage Buckets + FTP - check `templates/middleman/Jenkinsfile` and `var/PipelineMiddleman`
* Automated job generating w/ DSL seed job. check `Jenkinsfile` and `jobs/**/seed.groovy`

## Soon to be published
* Wordpress @ Kubernetes cluster
* Symfony @ Kubernetes cluster
* Nette @ Kubernetes cluster
* Backup pipeline jobs for mysql/mongodb pods in GKE
* Google compute snapshots pipeline job
* Android build job
* iOS build job
* Android/iOS/Node.js Merge Request builder jobs

# How to contribute
* feel free to create a pull request or submit an issue

Our production jenkins-pipeline-shared-library looks much different and is under development. I can't keep up with updating this upstream github repo. 

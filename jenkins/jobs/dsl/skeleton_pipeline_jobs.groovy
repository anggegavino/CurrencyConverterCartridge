def folder_name = "cartridge_folder"

folder( "${folder_name}" ){}

freeStyleJob("${folder_name}/Activity1") {

    // general
    properties {
        copyArtifactPermissionProperty {
        projectNames('Activity3')
        }
    }  

    // source code management  
    scm {
        git {
            remote {
                url('http://52.53.40.250/gitlab/Gavino/CurrencyConverterDTS.git')
                credentials('bc8a5297-21cd-498f-98bf-4a7a5d17815b')
            }
        }
    }

    // build triggers
     triggers {
        gitlabPush {
            buildOnMergeRequestEvents(true)
            buildOnPushEvents(true)
            enableCiSkip(false)
            setBuildDescription(false)
            rebuildOpenMergeRequest('never')
        }
    }
    wrappers {
        preBuildCleanup()
    }
  
    // build
    steps {
        maven{
            mavenInstallation('ADOP Maven')
            goals('package')
        }
    }

    // post build actions
     publishers {
        archiveArtifacts {
            pattern('**/*.war')
            onlyIfSuccessful()
        }
        downstream('Activity2', 'SUCCESS')
    }
}

freeStyleJob("${folder_name}/Activity2") {
    // source code management  
    scm {
        git {
            remote {
                url('http://52.53.40.250/gitlab/Gavino/CurrencyConverterDTS.git')
                credentials('bc8a5297-21cd-498f-98bf-4a7a5d17815b')
            }
        }
    }

    // build
    configure { project ->
        project / 'builders' / 'hudson.plugins.sonar.SonarRunnerBuilder' {
        properties('''
            sonar.projectKey=SonarActivityTest
            sonar.projectName=simulationActivity
            sonar.projectVersion=1.0
            sonar.sources=.
        ''')
        javaOpts()
        jdk('(Inherit From Job)')
        task()
        }
    }    

  //post build actions
  publishers {
        downstream('Activity3', 'SUCCESS')
    }

}

freeStyleJob("${folder_name}/Activity3") {

  // build copy artifacts

steps {
      copyArtifacts('Activity1') {
            includePatterns('target/*.war')
            flatten()
            optional()
            buildSelector {
                latestSuccessful(true)
            }
            fingerprintArtifacts()
        }
      nexusArtifactUploader {
        nexusVersion('NEXUS2')
        protocol('http')
        nexusUrl('nexus:8081/nexus')
        groupId('DTSActivity')
        version('1')
        repository('snapshots')
        credentialsId('bc8a5297-21cd-498f-98bf-4a7a5d17815b')
        artifact {
            artifactId('CurrencyConverter')
            type('war')
            file('/var/jenkins_home/jobs/Activity3/workspace/target/CurrencyConverter.war')
        }
      }
    }
  
  // post build actions
     publishers {
        archiveArtifacts {
            pattern('**/*.war')
            onlyIfSuccessful()
        }
        downstream('Activity4', 'UNSTABLE')
    }
}

freeStyleJob("${folder_name}/Activity4") {

  // label
  label('ansible')

  // source code management  
    scm {
        git {
            remote {
                url('http://52.53.40.250/gitlab/Gavino/Ansible.git')
                credentials('bc8a5297-21cd-498f-98bf-4a7a5d17815b')
            }
        }
    }

// build environment + bindings
wrappers {
        sshAgent('adop-jenkins-master')
        credentialsBinding {
            usernamePassword('username', 'password', 'credential')
        }
    }

// build- execute shell

steps {
        shell('''ls -la
                ansible-playbook -i hosts master.yml -u ec2-user -e "image_version=$BUILD_NUMBER username=$username password=$password"''')
    }

// post build actions
publishers {
        downstream('Activity5', 'SUCCESS')
    }
}

freeStyleJob("${folder_name}/Activity5") {

  // source code management  
    scm {
        git {
            remote {
                url('http://52.53.40.250/gitlab/Gavino/SeleniumDTS.git')
                credentials('bc8a5297-21cd-498f-98bf-4a7a5d17815b')
            }
        }
    }

    // build
    steps {
        maven{
            mavenInstallation('ADOP Maven')
            goals('test')
        }
    }
    // post build actions
     publishers {
        downstream('Activity6', 'SUCCESS')
    }
}

freeStyleJob("${folder_name}/Activity6") {

   // general
    properties {
        copyArtifactPermissionProperty {
        projectNames('Activity3')
        }
    }  

    // build
    steps {
        copyArtifacts('Activity3') {
            includePatterns('**/*.war')
            fingerprintArtifacts()
            buildSelector {
                latestSuccessful(true)
            }
        }
            
        nexusArtifactUploader {
            nexusVersion('NEXUS2')
            protocol('http')
            nexusUrl('nexus:8081/nexus')
            groupId('DTSActivity')
            version('${BUILD_NUMBER}')
            repository('releases')
            credentialsId('bc8a5297-21cd-498f-98bf-4a7a5d17815b')
            artifact {
                artifactId('CurrencyConverter')
                type('war')
                file('target/CurrencyConverter.war')
        }
      }
    }

}

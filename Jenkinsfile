#!groovy

node('slave1') {

		stage("Cleanup Workspace"){ deleteDir() }

		stage("Checkout"){ checkout scm }

/* Build  Artifacts  */
		stage("Build"){

    sh "ant -file build/build.xml continuous_build"
    }
  }
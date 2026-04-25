pipeline {
    agent any

    tools {
        jdk 'jdk-21'
        nodejs 'node-18'
    }

    environment {
        APP_NAME = 'tokyo-last-train'
        JAR_NAME = 'last-train-0.0.1-SNAPSHOT.jar'
        EC2_HOST = credentials('ec2-host')       // EC2 퍼블릭 IP 또는 도메인
        EC2_USER = 'ec2-user'
        SSH_KEY  = credentials('ec2-ssh-key')     // EC2 SSH 프라이빗 키
        ODPT_API_KEY = credentials('odpt-api-key')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build Frontend') {
            steps {
                dir('frontend') {
                    sh 'npm ci'
                    sh 'npm run build'
                }
            }
        }

        stage('Build Backend') {
            steps {
                // 프론트엔드 빌드 결과를 Spring Boot static 리소스로 복사
                sh 'mkdir -p src/main/resources/static'
                sh 'cp -r frontend/dist/* src/main/resources/static/'
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn test'
            }
        }

        stage('Deploy') {
            when {
                branch 'main'
            }
            steps {
                // JAR 파일을 EC2로 전송
                sh """
                    scp -i ${SSH_KEY} -o StrictHostKeyChecking=no \
                        target/${JAR_NAME} \
                        ${EC2_USER}@${EC2_HOST}:/opt/${APP_NAME}/app.jar.new
                """

                // 배포 스크립트 전송 및 실행
                sh """
                    scp -i ${SSH_KEY} -o StrictHostKeyChecking=no \
                        scripts/deploy.sh \
                        ${EC2_USER}@${EC2_HOST}:/opt/${APP_NAME}/deploy.sh
                """

                // 프론트엔드 정적 파일 전송 (Nginx용)
                sh """
                    ssh -i ${SSH_KEY} -o StrictHostKeyChecking=no \
                        ${EC2_USER}@${EC2_HOST} 'mkdir -p /opt/${APP_NAME}/frontend'
                    scp -i ${SSH_KEY} -o StrictHostKeyChecking=no \
                        -r frontend/dist/* \
                        ${EC2_USER}@${EC2_HOST}:/opt/${APP_NAME}/frontend/
                """

                sh """
                    ssh -i ${SSH_KEY} -o StrictHostKeyChecking=no \
                        ${EC2_USER}@${EC2_HOST} \
                        'chmod +x /opt/${APP_NAME}/deploy.sh && sudo /opt/${APP_NAME}/deploy.sh'
                """
            }
        }
    }

    post {
        success {
            echo "Deployment successful: ${APP_NAME}"
        }
        failure {
            echo "Pipeline failed: ${APP_NAME}"
        }
        always {
            cleanWs()
        }
    }
}
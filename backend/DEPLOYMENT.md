# Deployment Guide for AWS (EC2)

Since you have pushed your latest code to GitHub, here are the steps to deploy it on your AWS server.

## Prerequisites
- You have an AWS EC2 instance running (e.g., Ubuntu or Amazon Linux 2).
- You have SSH access to the instance.
- Port **8080** is open in your instance's Security Group (Inbound rules).

---

## Option 1: Using Docker (Recommended)
This is the easiest method because your project already has a `Dockerfile`.

1. **Connect to your AWS instance**:
   ```bash
   ssh -i your-key.pem ubuntu@your-ec2-ip
   ```

2. **Install Docker** (if not installed):
   ```bash
   # For Ubuntu
   sudo apt-get update
   sudo apt-get install -y docker.io
   # Start docker
   sudo systemctl start docker
   sudo systemctl enable docker
   ```

3. **Clone your code**:
   ```bash
   git clone https://github.com/YOUR_USERNAME/YOUR_REPO_NAME.git
   cd YOUR_REPO_NAME
   ```

4. **Build the Docker image**:
   ```bash
   sudo docker build -t grc-service .
   ```

5. **Run the container**:
   ```bash
   sudo docker run -d -p 8080:8080 --name grc-app \
     -e SPRING_DATASOURCE_PASSWORD=AVNS_UQ9DrYwkQYLtU2x1ELe \
     grc-service
   ```
   *(Note: The `-d` flag runs it in the background. We passed the DB password solely as an example; strictly speaking, it's safer to not hardcode it, but your default `application.properties` also has defaults.)*

---

## Option 2: Running Manually (Java JAR)

1. **Install Java 17 and Maven**:
   ```bash
   # For Ubuntu
   sudo apt-get update
   sudo apt-get install -y openjdk-17-jdk maven
   ```

2. **Clone and Build**:
   ```bash
   git clone https://github.com/YOUR_USERNAME/YOUR_REPO_NAME.git
   cd YOUR_REPO_NAME
   mvn clean package -DskipTests
   ```

3. **Run the Application**:
   ```bash
   # Simple run (stops when you close terminal)
   java -jar target/grc-service-0.0.1-SNAPSHOT.jar

   # OR Run in background (keeps running after you exit)
   nohup java -jar target/grc-service-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
   ```

---

## Verification

Check if it is running by accessing your EC2 IP:
`http://YOUR_EC2_IP:8080/api/health` (or any endpoint you have).

If it fails:
- Check logs: `docker logs grc-app` (if using Docker) or `cat app.log` (if manual).
- Ensure **Security Groups** in AWS Console allow traffic on port 8080.

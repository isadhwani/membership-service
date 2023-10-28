#FROM amazoncorretto:17
#
##RUN apt-get update
##RUN apt-get install -y gcc
##
##RUN apt-get install -y netcat
##RUN apt-get install -y iputils-ping
#
##RUN apt install default-jre
#
#
#WORKDIR /app/
#
#ADD pom.xml /app/
#ADD src /app/src/
#ADD target /app/target/
#
#
#RUN wget https://mirrors.estointernet.in/apache/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz \
#RUN tar -xvf apache-maven-3.6.3-bin.tar.gz
#RUN mv apache-maven-3.6.3 /opt/
#
#RUN M2_HOME='/opt/apache-maven-3.6.3' \
#RUN PATH="$M2_HOME/bin:$PATH"
#RUN export PATH
#
#RUN mvn package
#
#ENTRYPOINT ["java", "-cp", "target/membership-service-1.0-SNAPSHOT.jar", "com.mycompany.app.App"]


FROM amazoncorretto:17


WORKDIR /app/

ADD src/main/java/local/Main.java /app/local/
ADD src/main/java/failureDetector/TimeoutData.java /app/failureDetector/
ADD src/main/java/failureDetector/UDPBroadcastHeartbeat.java /app/failureDetector/
ADD src/main/java/failureDetector/HandleTimeout.java /app/failureDetector/
ADD src/main/java/failureDetector/UDPListenHeartbeat.java /app/failureDetector/
ADD src/main/java/failureDetector/FailureDetector.java /app/failureDetector/
ADD src/main/java/local/LeaderValues.java /app/local/
ADD src/main/java/local/StateValue.java /app/local/
ADD src/main/java/local/TCPConnection.java /app/local/
ADD src/main/java/local/TCPListener.java /app/local/
ADD src/main/java/local/TCPTalker.java /app/local/


ADD /docker-compose-testcases-and-hostsfile-lab3/hostsfile.txt /app/

RUN javac /app/failureDetector/TimeoutData.java
RUN javac /app/failureDetector/HandleTimeout.java
RUN javac /app/failureDetector/UDPBroadcastHeartbeat.java
RUN javac /app/failureDetector/UDPListenHeartbeat.java
RUN javac /app/failureDetector/FailureDetector.java
RUN javac /app/local/LeaderValues.java
RUN javac /app/local/StateValue.java
RUN javac /app/local/TCPListener.java
RUN javac /app/local/TCPTalker.java
RUN javac /app/local/Main.java
RUN javac /app/local/TCPConnection.java


ENTRYPOINT ["java", "local/Main"]


FROM maven:3.6.3-openjdk-15
RUN mkdir /app
WORKDIR /app
COPY . /app
RUN mvn clean install -DskipTests
#RUN mvn exec:java -Dexec.mainClass="benchmark.Neo4jBenchmark" -Dexec.args="bt 1 2 5 /Users/vladapogozhelskaya/Downloads/neo4j-enterprise-4.0.12 test/resources/grammars/graph/Test1/grammar.json geospecies" -Dexec.cleanupDaemonThreads=false
RUN chmod +x entrypoint.sh
# ENTRYPOINT ["/bin/bash", "entrypoint.sh"]


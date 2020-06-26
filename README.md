Test Containers
---
Current Code:
1. Using MySQL in Container mode with JDBC string way
1. Using Redis in Container mode with Generic Container way
1. Execute integration test against these containers instead of local machine installation
1. Validation that data saved in DB and Cache as well
1. Validation that data once saved in Cache will be returned from Cache not from DB
---
Important Points:
* Test containers are used for run integration test in java where all supporting components are containerized and tests will run against these containers.
* No need to mock these dependencies or locally install on your machine or CI machine
* Dependencies 
```groovy
testCompile "org.testcontainers:testcontainers:1.14.3"
testCompile "org.testcontainers:junit-jupiter:1.14.3"
```
* Annotate the test class with @Testcontainers
* Add following line for Redis container
```java
@Container
public GenericContainer redis = new GenericContainer<>("redis:5.0.3-alpine")
                                        .withExposedPorts(6379);
```
* Testcontainers uses randomized ports for each container it starts, but makes it easy to obtain the actual port at runtime by using `container.getMappedPort()`.  
* Also possible to expose port on HOST machine to TestContainer. Eg : Selenium test container need to know about application PORT. `Testcontainers.exposeHostPorts(localServerPort);`
* These containers will be started before any tests in the class run, and will be destroyed after all tests have run.
* Executing commands
    * ` .withCommand("redis-server --port 7777")` while creation with Generic container
    * on running container - `Container.ExecResult lsResult = container.execInContainer("touch", "/somefile.txt");` and can get output
    * set env variable - `.withEnv("API_TOKEN", "foo")`
* Volume mapping (file/dir on classpath will be mapped ) - `.withClasspathResourceMapping("redis.conf", "/etc/redis.conf", BindMode.READ_ONLY)`     
* Container Readiness waiting
    * Wait Strategy
        * default 60 sec wait for port to ready to listen
        * can be changed by `withStartupTimeout()`
        * Can add other wait strategy like 
            * `.waitingFor(Wait.forHttp("/"));`
            *  `Wait.forHttp("/").forStatusCode(200)`
            * Log message from container
            * Docker health check - `Wait.forHealthcheck()`
    * Startup Strategy
        * Testcontainers will check that the container has reached the running state and has not exited
        * Running startup strategy - container is running or not (default)
        * One shot startup strategy - start and stop container
        * Indefinite one shot startup strategy - long running task in container
* Container Logs
    * Container output will always begin from the time of container creation.
        * Snapshot Logs - `final String logs = container.getLogs();` snapshot of entire container log
        * Streaming logs
            * Different kind of consumer can be added along with STDOUT/STDERR or both
            * `Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(LOGGER);  
                            container.followOutput(logConsumer);`
            * `ToStringConsumer toStringConsumer = new ToStringConsumer();`  
              `container.followOutput(toStringConsumer, OutputType.STDOUT);`
            * Waiting consumer
* Can create Docker image on fly for those for which Docker image is not created
    * 
    ```java
    public GenericContainer dslContainer = new GenericContainer(
        new ImageFromDockerfile()
                .withFileFromString("folder/someFile.txt", "hello")
                .withFileFromClasspath("test.txt", "mappable-resource/test-resource.txt")
                .withFileFromClasspath("Dockerfile", "mappable-dockerfile/Dockerfile"))
    ```                                         
    * 
   ```java
   new ImageFromDockerfile()
           .withDockerfileFromBuilder(builder ->
                   builder
                           .from("alpine:3.2")
                           .run("apk add --update nginx")
                           .cmd("nginx", "-g", "daemon off;")
                           .build()))
           .withExposedPorts(80); 
    ```
   * These docker files will be automatically deleted, but you can customise it
   * Other docker file path can be specified
* Testcontainer properties can be customised by defining a file named 'testcontainer.properties' in class path for disabling checks, time out for pull, customise images
* ` .withImagePullPolicy(PullPolicy.alwaysPull())`
* Database container
    * URL Based
        * `spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver`
        * Insert tc: after jdbc: as follows. Note that the hostname, port and database name will be ignored; `jdbc:tc:mysql:5.6.23:///databasename`
        * Classpath - `jdbc:tc:mysql:5.7.22:///databasename?TC_INITSCRIPT=somepath/init_mysql.sql`, file one can also be specified
        * `jdbc:tc:mysql:5.7.22:///databasename?TC_INITFUNCTION=org.testcontainers.jdbc.JDBCDriverTest::sampleInitFunction`
    * Object based  
        ` @Container public MySQLContainer mysql = new MySQLContainer();`  
         ` @Container private PostgreSQLContainer postgresqlContainer = new PostgreSQLContainer().withDatabaseName("foo").withUsername("foo").withPassword("secret");`     
    * Add module in build like `testCompile "org.testcontainers:mysql:1.14.3"` 
* Container sharing
    * Restarted - containers that are restarted for every test method - instance 
    * Shared - containers that are shared between all methods of a test class - static variables
    * Singleton container - defined in singletong class
* You can call start and stop on container    
---
References:
> https://www.testcontainers.org/

Readme.txt
1. Install lombok.jar in your eclipse (refer to the instructions here https://projectlombok.org)

2. Add the following to the build.sbt of the parent project to create a JAR

lazy val JDBCAdapters = project.in(file("InputOutputAdapters/JDBCAdapters")) dependsOn(InputOutputAdapterBase, Exceptions, DataDelimiters)
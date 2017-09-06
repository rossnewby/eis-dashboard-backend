# EIS Quality Assurance Dashboard - Backend

Java application which analyses raw data from CKAN, providing back end support for the EIS Dashboard front-end found [here](https://github.com/oscarechobravo/eis-dashboard)

## Setup

Setupy MySQL DB*

Add a config.properties file to source directory (src) of the form:

```properties

apikey=<insert your ckan api key>
apiuser=<insert ckan api basic auth username>
apipass=<insert ckan api basic auth password>
mysqluser=root
mysqlpass=<insert your mysql root password; default = "">

```

## Dependancies

JSON in Java (Version 20160810): [Downloadable Maven Repository](https://mvnrepository.com/artifact/org.json/json)

MySQL: [Javadoc](https://docs.oracle.com/javase/7/docs/api/java/sql/package-summary.html)

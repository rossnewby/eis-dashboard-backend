# EIS Quality Assurance Dashboard - Backend

<img align="right" height="180"  src="http://cdn-edu.wpmhost.net/53544f/707f375833/72C5-6F0E-57A0-5A9A-0718.jpg"/>

The Energy Information System (EIS) Quality Assurance Dashboard is designed to facilitate experts at Lancaster University. These experts include building managers and energy managers, as well as others who wish to gain knowledge of data quality performance. To achieve this, the backend Java application found here; analyses large quantities of raw data in CKAN, which stores historical Building Management System (BMS) and Energy Management System (EMS) data. This supports the front-end dashbaord application; found [here](https://github.com/oscarechobravo/eis-dashboard). 

## Getting Started

### MySQL

MySQL must be running for the application to create and update a database, the community edition can be [downloaded here](https://dev.mysql.com/downloads/) for all operating systems

### Dependancies

Below are the dependencies that must be added to the project. This can be done in IntelliJ IDEA through `File > Project Structure > Modules > Add > JARs or directories` and then selecting the folling JAR files:

JSON in Java (Version 20160810 or later): [Download](https://mvnrepository.com/artifact/org.json/json) / [Javadoc](https://developer.android.com/reference/org/json/package-summary.html)

MySQL (installing the community edition MySQL package above will install this also): [Download](https://dev.mysql.com/downloads/connector/j/) / [Javadoc](https://docs.oracle.com/javase/7/docs/api/java/sql/package-summary.html)

### Configuration File

Add a config.properties file to the project source directory (src) which includes the following fields:

```properties
apikey=<insert your ckan api key>
apiuser=<insert ckan api basic auth username>
apipass=<insert ckan api basic auth password>
mysqluser=root
mysqlpass=<insert your mysql root password; default = "">
```

<img align="left" height="100"  src="https://avatars1.githubusercontent.com/u/1630326?v=4&s=400"/>

The CKAN basic authentication username and password can be found through lancaster EIS staff and the private project repository. Authentication credentials should not be shared or uploaded to the public git repository; please take extra care when creating your config file. Your API key can be found through the [data portal](https://ckan.lancaster.ac.uk), you must login on request an account through ISS.

## Authors

> **Ross Newby** - *Initial Work* - [rossnewby](https://github.com/rossnewby)

> **Oliver Bates** - *Project Support* - [oscarechobravo](https://github.com/oscarechobravo)

See also the list of [contributors](https://github.com/rossnewby/eis-dashboard-backend/contributors) who participated in this project.

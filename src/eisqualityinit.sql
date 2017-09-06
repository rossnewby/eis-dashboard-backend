DROP DATABASE IF EXISTS eisquality;
CREATE DATABASE eisquality;
USE eisquality;

DROP TABLE IF EXISTS erroneousassets ;
CREATE TABLE erroneousassets
(
  id int unsigned NOT NULL auto_increment,
  hardware varchar(25),
  logger_code varchar(100) NOT NULL,
  logger_channel varchar(10),
  utility_type varchar(25),

  PRIMARY KEY (id),
  UNIQUE KEY id_and_logger_info (id, logger_code, logger_channel)
);

DROP TABLE IF EXISTS metadataerrors;
CREATE TABLE metadataerrors
(
  id int unsigned NOT NULL auto_increment,
  error_type int NOT NULL,
  logger_code varchar(100) NOT NULL,
  logger_channel varchar(10),

  PRIMARY KEY (id)
);

DROP TABLE IF EXISTS errors;
CREATE TABLE errors
(
  id int unsigned NOT NULL auto_increment,
  error_type int NOT NULL,
  logger_code varchar(100) NOT NULL,
  logger_channel varchar(10) NOT NULL,
  timeVal timestamp,

  PRIMARY KEY (id)
);

DROP TABLE IF EXISTS errortypelookup;
CREATE TABLE errortypelookup
(
    id int NOT NULL auto_increment,
    description varchar(255) NOT NULL,

    PRIMARY KEY (id)
);

INSERT INTO errortypes VALUES (1, "Logger in metadata has no associated meters in metadata");
INSERT INTO errortypes VALUES (2, "Logger with missing data fields in its metadata");
INSERT INTO errortypes VALUES (3, "Meters in metadata has no associated loggers in metadata");
INSERT INTO errortypes VALUES (4, "Meter with missing date fields in its metadata");
INSERT INTO errortypes VALUES (5, "Meter with no recent data readings");
INSERT INTO errortypes VALUES (6, "Meter with negative data reading");
INSERT INTO errortypes VALUES (7, "Meter with a non-normal data reading");
INSERT INTO errortypes VALUES (8, "Meter with incorrect time interval between readings");
INSERT INTO errortypes VALUES (9, "Meter with no data reading at all");


DROP DATABASE IF EXISTS eisquality;
CREATE DATABASE eisquality;
USE eisquality;

DROP TABLE IF EXISTS erroneousassets ;
CREATE TABLE erroneousassets
(
  id int unsigned NOT NULL auto_increment,
  hardware varchar(25) NOT NULL,
  logger_code varchar(100) NOT NULL,
  logger_channel varchar(10),
  utility_type varchar(25),
  most_recent_error timestamp NOT NULL,

  PRIMARY KEY (id),
  UNIQUE KEY id_and_logger_info (id, logger_code, logger_channel)
);

DROP TABLE IF EXISTS errors;
CREATE TABLE errors
(
  id int unsigned NOT NULL auto_increment,
  error_type int NOT NULL,
  logger_code varchar(100) NOT NULL,
  logger_channel varchar(10),
  timeVal timestamp,

  PRIMARY KEY (id)
);

DROP TABLE IF EXISTS qualitylog;
CREATE TABLE qualitylog
(
    id int unsigned NOT NULL auto_increment,
    total_assets int NOT NULL,
    erroneous_assets int NOT NULL,
    error_count int NOT NULL,
    timeVal timestamp NOT NULL,

    PRIMARY KEY (id)
);

DROP TABLE IF EXISTS errortypelookup;
CREATE TABLE errortypelookup
(
    id int NOT NULL auto_increment,
    description varchar(255) NOT NULL,

    PRIMARY KEY (id)
);

INSERT INTO errortypelookup VALUES (1, "Logger in metadata has no associated meters in metadata");
INSERT INTO errortypelookup VALUES (2, "Logger with missing building code in its metadata");
INSERT INTO errortypelookup VALUES (3, "Logger with missing description in its metadata");

INSERT INTO errortypelookup VALUES (10, "Meters in metadata has no associated loggers in metadata");
INSERT INTO errortypelookup VALUES (11, "Meter with missing asset code in its metadata");
INSERT INTO errortypelookup VALUES (12, "Meter with missing description in its metadata");

INSERT INTO errortypelookup VALUES (20, "Meter with no data reading at all");
INSERT INTO errortypelookup VALUES (21, "Meter with no recent data readings");

INSERT INTO errortypelookup VALUES (22, "Meter with negative data reading");
INSERT INTO errortypelookup VALUES (23, "Meter with a non-normal data reading");
INSERT INTO errortypelookup VALUES (24, "Meter with incorrect time interval between readings");



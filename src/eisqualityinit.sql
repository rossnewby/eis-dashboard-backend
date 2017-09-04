USE eisquality;

DROP TABLE IF EXISTS erroneousassets ;
CREATE TABLE erroneousassets
(
  id int unsigned NOT NULL auto_increment,
  logger_code varchar(255) NOT NULL,
  logger_channel varchar(255),
  building_code varchar(255),
  description varchar(255) NOT NULL,
  utility_type varchar(255),
  additional_loc_info varchar(255),
  asset_code varchar(255) NOT NULL,

  PRIMARY KEY (id)
);

DROP TABLE IF EXISTS metadataerrors;
CREATE TABLE metadataerrors
(
  id int unsigned NOT NULL auto_increment,
  error_type varchar(255) NOT NULL,
  asset_id int unsigned NOT NULL,
  asset_code varchar(255) NOT NULL,
  description varchar(255) NOT NULL,

  PRIMARY KEY (id)
);

DROP TABLE IF EXISTS metererrors;
CREATE TABLE metererrors
(
  id int unsigned NOT NULL auto_increment,
  error_type varchar(255) NOT NULL,
  asset_id int unsigned NOT NULL,
  asset_code varchar(255) NOT NULL,
  repetitions decimal(10,2) NOT NULL,

  PRIMARY KEY (id)
);

-- INSERT INTO errors (error_type, repetitions) VALUES ("test val", 10);


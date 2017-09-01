USE eisquality;

DROP TABLE IF EXISTS errors;
CREATE TABLE errors
(
  id int unsigned NOT NULL auto_increment,
  error_type varchar(255) NOT NULL,
  repetitions decimal(10,2) NOT NULL,

  PRIMARY KEY (id)
);

DROP TABLE IF EXISTS metadataerrors;
CREATE TABLE metadataerrors
(
  id int unsigned NOT NULL auto_increment,
  error_type varchar(255) NOT NULL,
  asset_code varchar(255) NOT NULL,
  description varchar(255) NOT NULL,

  PRIMARY KEY (id)
);

INSERT INTO errors (error_type, repetitions) VALUES ("test val", 10);


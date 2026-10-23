create table data_source_definition (
  id uuid primary key,
  name varchar(120) not null unique,
  type varchar(30) not null,
  jdbc_url varchar(1000) not null,
  username varchar(255) not null,
  credential_reference varchar(500),
  created_at timestamp with time zone not null
);

create table sql_dataset (
  id uuid primary key,
  name varchar(120) not null unique,
  data_source_id uuid not null references data_source_definition(id),
  sql_text text not null,
  created_at timestamp with time zone not null
);

create index idx_sql_dataset_data_source on sql_dataset(data_source_id);


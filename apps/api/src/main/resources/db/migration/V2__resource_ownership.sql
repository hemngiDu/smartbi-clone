alter table data_source_definition add column owner_id varchar(120) not null default 'legacy-admin';
alter table sql_dataset add column owner_id varchar(120) not null default 'legacy-admin';
alter table data_source_definition alter column owner_id drop default;
alter table sql_dataset alter column owner_id drop default;
create index idx_data_source_owner on data_source_definition(owner_id);
create index idx_sql_dataset_owner on sql_dataset(owner_id);

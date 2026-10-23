create role analytics_reader login password 'compose_reader_only';
create table sales (id bigint generated always as identity primary key, region text not null, amount numeric(12,2) not null);
insert into sales(region,amount) values ('华东',188.50),('华南',92.00),('华北',305.20);
grant connect on database analytics to analytics_reader;
grant usage on schema public to analytics_reader;
grant select on all tables in schema public to analytics_reader;
alter default privileges in schema public grant select on tables to analytics_reader;
alter role analytics_reader set default_transaction_read_only = on;

create table report_template (
  id uuid primary key,
  name varchar(120) not null,
  owner_id varchar(120) not null,
  definition_json text not null,
  created_at timestamp with time zone not null,
  updated_at timestamp with time zone not null,
  constraint uq_report_template_owner_name unique (owner_id, name)
);
create index idx_report_template_owner on report_template(owner_id);

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

create table if not exists maas_platform(
    maas_platform_id varchar(255) primary key,
    maas_platform_name varchar(255),
    operator_id varchar(255),
    operator_name varchar(255),
    maas_type varchar(255),
    server_ip varchar(255),
    vector_model varchar(255)
);

create table if not exists model_information(
    model_id varchar(255) primary key,
    model_name varchar(255),
    maas_platform_id varchar(255)
);

create table if not exists knowledge_base(
    knowledge_base_id varchar(255) primary key,
    knowledge_base_name varchar(255),
    knowledge_base_description VARCHAR (255),
    operator_id varchar(255),
    operator_name varchar(255),
    maas_platform_id varchar(255),
    maas_platform_name varchar(255),
    update_time timestamptz
);

create table if not exists file(
    file_id varchar(255) primary key,
    file_name varchar(255),
    knowledge_base_id varchar(255)
);

create table if not exists application(
    application_id varchar(255) primary key,
    application_name varchar(255),
    application_description varchar(255),
    application_type varchar(255),
    knowledge_base_id varchar(255),
    model_id varchar(255),
    model_name varchar(255),
    prompt varchar(1000),
    temperature float,
    top_p float,
    opening_remarks varchar(500)
)


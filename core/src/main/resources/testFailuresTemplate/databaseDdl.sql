create table job_view (
    id identity primary key,
    name varchar(128) NOT NULL,
    url varchar(256) NOT NULL,
    last_fetch_amount int NOT NULL
);

create table job (
    id identity primary key,
    view_id bigint NOT NULL references job_view(id),
    name varchar(128) NOT NULL,
    url varchar(256) NOT NULL
);

CREATE UNIQUE INDEX idx_job_url ON job(url);

create table job_build (
    id identity primary key,
    job_id bigint NOT NULL references job(id),
    name varchar(128) NOT NULL,
    url varchar(256) NOT NULL,
    build_number int NOT NULL,
    commit_id varchar(48) NOT NULL,
    status varchar(32) NOT NULL,
    failed_count int NOT NULL,
    skipped_count int NOT NULL,
    build_timestamp bigint NOT NULL
);

create table test_result (
    id identity primary key,
    job_build_id bigint NOT NULL references job_build(id),
    name varchar(128) NOT NULL,
    status varchar(32) NOT NULL,
    failed_builds int array,
    skipped_builds int array,
    passed_builds int array,
    presumed_passed_builds int array,
    class_name varchar(128) NOT NULL,
    parameters varchar array,
    package_path varchar(128) NOT NULL,
    exception varchar,
    duration double NOT NULL,
    started_at bigint NOT NULL,
    similar_skips int,
    data_provider_index int
);
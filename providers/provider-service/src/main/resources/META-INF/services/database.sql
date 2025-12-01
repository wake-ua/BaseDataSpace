--
-- PostgreSQL database
--

CREATE DATABASE edc_service WITH ENCODING = 'UTF8';
\connect edc_service


CREATE TABLE public.edc_asset (
    asset_id character varying NOT NULL,
    created_at bigint NOT NULL,
    properties json DEFAULT '{}'::json,
    private_properties json DEFAULT '{}'::json,
    data_address json DEFAULT '{}'::json
);
COMMENT ON COLUMN public.edc_asset.properties IS 'Asset properties serialized as JSON';
COMMENT ON COLUMN public.edc_asset.private_properties IS 'Asset private properties serialized as JSON';
COMMENT ON COLUMN public.edc_asset.data_address IS 'Asset DataAddress serialized as JSON';


CREATE TABLE public.edc_contract_agreement (
    agr_id character varying NOT NULL,
    provider_agent_id character varying,
    consumer_agent_id character varying,
    signing_date bigint,
    start_date bigint,
    end_date integer,
    asset_id character varying NOT NULL,
    policy json
);


CREATE TABLE public.edc_contract_definitions (
    created_at bigint NOT NULL,
    contract_definition_id character varying NOT NULL,
    access_policy_id character varying NOT NULL,
    contract_policy_id character varying NOT NULL,
    assets_selector json NOT NULL,
    private_properties json
);


CREATE TABLE public.edc_contract_negotiation (
    id character varying NOT NULL,
    created_at bigint NOT NULL,
    updated_at bigint NOT NULL,
    correlation_id character varying,
    counterparty_id character varying NOT NULL,
    counterparty_address character varying NOT NULL,
    protocol character varying NOT NULL,
    type character varying NOT NULL,
    state integer DEFAULT 0 NOT NULL,
    state_count integer DEFAULT 0,
    state_timestamp bigint,
    error_detail character varying,
    agreement_id character varying,
    contract_offers json,
    callback_addresses json,
    trace_context json,
    pending boolean DEFAULT false,
    protocol_messages json,
    lease_id character varying
);
COMMENT ON COLUMN public.edc_contract_negotiation.agreement_id IS 'ContractAgreement serialized as JSON';
COMMENT ON COLUMN public.edc_contract_negotiation.contract_offers IS 'List<ContractOffer> serialized as JSON';
COMMENT ON COLUMN public.edc_contract_negotiation.trace_context IS 'Map<String,String> serialized as JSON';


CREATE TABLE public.edc_lease (
    leased_by character varying NOT NULL,
    leased_at bigint,
    lease_duration integer DEFAULT 60000 NOT NULL,
    lease_id character varying NOT NULL
);
COMMENT ON COLUMN public.edc_lease.leased_at IS 'posix timestamp of lease';
COMMENT ON COLUMN public.edc_lease.lease_duration IS 'duration of lease in milliseconds';


CREATE TABLE public.edc_policydefinitions (
    policy_id character varying NOT NULL,
    created_at bigint NOT NULL,
    permissions json,
    prohibitions json,
    duties json,
    profiles json,
    extensible_properties json,
    inherits_from character varying,
    assigner character varying,
    assignee character varying,
    target character varying,
    policy_type character varying NOT NULL,
    private_properties json
);
COMMENT ON COLUMN public.edc_policydefinitions.permissions IS 'Java List<Permission> serialized as JSON';
COMMENT ON COLUMN public.edc_policydefinitions.prohibitions IS 'Java List<Prohibition> serialized as JSON';
COMMENT ON COLUMN public.edc_policydefinitions.duties IS 'Java List<Duty> serialized as JSON';
COMMENT ON COLUMN public.edc_policydefinitions.profiles IS 'Java List<String> serialized as JSON';
COMMENT ON COLUMN public.edc_policydefinitions.extensible_properties IS 'Java Map<String, Object> serialized as JSON';
COMMENT ON COLUMN public.edc_policydefinitions.policy_type IS 'Java PolicyType serialized as JSON';


CREATE TABLE public.edc_transfer_process (
    transferprocess_id character varying NOT NULL,
    type character varying NOT NULL,
    state integer NOT NULL,
    state_count integer DEFAULT 0 NOT NULL,
    state_time_stamp bigint,
    created_at bigint NOT NULL,
    updated_at bigint NOT NULL,
    trace_context json,
    error_detail character varying,
    resource_manifest json,
    provisioned_resource_set json,
    content_data_address json,
    deprovisioned_resources json,
    private_properties json,
    callback_addresses json,
    pending boolean DEFAULT false,
    transfer_type character varying,
    protocol_messages json,
    data_plane_id character varying,
    correlation_id character varying,
    counter_party_address character varying,
    protocol character varying,
    asset_id character varying,
    contract_id character varying,
    data_destination json,
    lease_id character varying
);
COMMENT ON COLUMN public.edc_transfer_process.trace_context IS 'Java Map serialized as JSON';
COMMENT ON COLUMN public.edc_transfer_process.resource_manifest IS 'java ResourceManifest serialized as JSON';
COMMENT ON COLUMN public.edc_transfer_process.provisioned_resource_set IS 'ProvisionedResourceSet serialized as JSON';
COMMENT ON COLUMN public.edc_transfer_process.content_data_address IS 'DataAddress serialized as JSON';
COMMENT ON COLUMN public.edc_transfer_process.deprovisioned_resources IS 'List of deprovisioned resources, serialized as JSON';

ALTER TABLE ONLY public.edc_contract_agreement
    ADD CONSTRAINT contract_agreement_pk PRIMARY KEY (agr_id);
ALTER TABLE ONLY public.edc_contract_negotiation
    ADD CONSTRAINT contract_negotiation_pk PRIMARY KEY (id);
ALTER TABLE ONLY public.edc_asset
    ADD CONSTRAINT edc_asset_pkey PRIMARY KEY (asset_id);
ALTER TABLE ONLY public.edc_contract_definitions
    ADD CONSTRAINT edc_contract_definitions_pkey PRIMARY KEY (contract_definition_id);
ALTER TABLE ONLY public.edc_policydefinitions
    ADD CONSTRAINT edc_policydefinitions_pkey PRIMARY KEY (policy_id);
ALTER TABLE ONLY public.edc_lease
    ADD CONSTRAINT lease_pk PRIMARY KEY (lease_id);
ALTER TABLE ONLY public.edc_transfer_process
    ADD CONSTRAINT transfer_process_pk PRIMARY KEY (transferprocess_id);

CREATE UNIQUE INDEX contract_agreement_id_uindex ON public.edc_contract_agreement USING btree (agr_id);
CREATE INDEX contract_negotiation_correlationid_index ON public.edc_contract_negotiation USING btree (correlation_id);
CREATE UNIQUE INDEX contract_negotiation_id_uindex ON public.edc_contract_negotiation USING btree (id);
CREATE INDEX contract_negotiation_state ON public.edc_contract_negotiation USING btree (state, state_timestamp);
CREATE INDEX transfer_process_state ON public.edc_transfer_process USING btree (state, state_time_stamp);
CREATE UNIQUE INDEX edc_policydefinitions_id_uindex ON public.edc_policydefinitions USING btree (policy_id);
CREATE UNIQUE INDEX lease_lease_id_uindex ON public.edc_lease USING btree (lease_id);
CREATE UNIQUE INDEX transfer_process_id_uindex ON public.edc_transfer_process USING btree (transferprocess_id);

ALTER TABLE ONLY public.edc_contract_negotiation
    ADD CONSTRAINT contract_negotiation_contract_agreement_id_fk FOREIGN KEY (agreement_id) REFERENCES public.edc_contract_agreement(agr_id);
ALTER TABLE ONLY public.edc_contract_negotiation
    ADD CONSTRAINT contract_negotiation_lease_lease_id_fk FOREIGN KEY (lease_id) REFERENCES public.edc_lease(lease_id) ON DELETE SET NULL;
ALTER TABLE ONLY public.edc_transfer_process
    ADD CONSTRAINT transfer_process_lease_lease_id_fk FOREIGN KEY (lease_id) REFERENCES public.edc_lease(lease_id) ON DELETE SET NULL;


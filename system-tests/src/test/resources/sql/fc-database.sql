

CREATE TABLE IF NOT EXISTS public.edc_federated_catalog
(
    id character varying COLLATE pg_catalog."default" NOT NULL,
    catalog json,
    marked boolean DEFAULT false,
    CONSTRAINT edc_federated_catalog_pkey PRIMARY KEY (id)
);
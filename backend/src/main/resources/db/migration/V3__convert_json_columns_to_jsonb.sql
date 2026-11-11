-- Convert TEXT columns to native PostgreSQL JSONB safely and idempotently

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'messages' AND column_name = 'metadata' AND data_type = 'text'
    ) THEN
        ALTER TABLE messages ALTER COLUMN metadata TYPE jsonb USING CASE
            WHEN metadata IS NULL OR btrim(metadata) = '' THEN NULL
            ELSE metadata::jsonb
        END;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'tool_executions' AND column_name = 'input_data' AND data_type = 'text'
    ) THEN
        ALTER TABLE tool_executions ALTER COLUMN input_data TYPE jsonb USING CASE
            WHEN input_data IS NULL OR btrim(input_data) = '' THEN NULL
            ELSE input_data::jsonb
        END;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'tool_executions' AND column_name = 'output_data' AND data_type = 'text'
    ) THEN
        ALTER TABLE tool_executions ALTER COLUMN output_data TYPE jsonb USING CASE
            WHEN output_data IS NULL OR btrim(output_data) = '' THEN NULL
            ELSE output_data::jsonb
        END;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'document_chunks' AND column_name = 'metadata' AND data_type = 'text'
    ) THEN
        ALTER TABLE document_chunks ALTER COLUMN metadata TYPE jsonb USING CASE
            WHEN metadata IS NULL OR btrim(metadata) = '' THEN NULL
            ELSE metadata::jsonb
        END;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'model_configs' AND column_name = 'configuration' AND data_type = 'text'
    ) THEN
        ALTER TABLE model_configs ALTER COLUMN configuration TYPE jsonb USING CASE
            WHEN configuration IS NULL OR btrim(configuration) = '' THEN NULL
            ELSE configuration::jsonb
        END;
    END IF;
END $$;

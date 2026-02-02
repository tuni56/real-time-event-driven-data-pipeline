-- Initialize database for data pipeline
CREATE SCHEMA IF NOT EXISTS pipeline;

-- Events table to store processed events
CREATE TABLE IF NOT EXISTS pipeline.events (
    id SERIAL PRIMARY KEY,
    event_id VARCHAR(255) UNIQUE NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    source_topic VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Processing status table
CREATE TABLE IF NOT EXISTS pipeline.processing_status (
    id SERIAL PRIMARY KEY,
    event_id VARCHAR(255) REFERENCES pipeline.events(event_id),
    status VARCHAR(50) NOT NULL,
    error_message TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_events_event_type ON pipeline.events(event_type);
CREATE INDEX IF NOT EXISTS idx_events_created_at ON pipeline.events(created_at);
CREATE INDEX IF NOT EXISTS idx_processing_status_event_id ON pipeline.processing_status(event_id);

-- Grant permissions
GRANT ALL PRIVILEGES ON SCHEMA pipeline TO pipeline_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA pipeline TO pipeline_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA pipeline TO pipeline_user;

-- Idempotent upgrade steps run after Hibernate's ddl-auto=update (H2 PostgreSQL mode and PostgreSQL).
-- ddl-auto never relaxes NOT NULL on existing columns, so do it explicitly for YOUTUBE songs.
ALTER TABLE song ALTER COLUMN audio_path DROP NOT NULL;
ALTER TABLE song ALTER COLUMN artist_id DROP NOT NULL;
-- Rows created before the source column existed are local files.
UPDATE song SET source = 'LOCAL' WHERE source IS NULL;
-- One Song row per YouTube video (NULLs are allowed repeatedly for LOCAL songs).
CREATE UNIQUE INDEX IF NOT EXISTS uq_song_external_id ON song (external_id);

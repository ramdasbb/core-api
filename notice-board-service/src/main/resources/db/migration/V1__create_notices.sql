-- Flyway migration: create notices table
CREATE TABLE IF NOT EXISTS notices (
  id UUID PRIMARY KEY,
  village_id UUID,
  title TEXT NOT NULL,
  notice_date DATE,
  category TEXT,
  description TEXT,
  attachment_url TEXT,
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now()
);

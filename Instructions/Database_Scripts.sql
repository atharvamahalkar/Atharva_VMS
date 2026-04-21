-- Architectural Ledger VMS Portal — PostgreSQL 15 Schema
-- Run: psql -U postgres -d vendor_management -f schema.sql

CREATE TABLE IF NOT EXISTS users (
  user_id            SERIAL PRIMARY KEY,
  username           VARCHAR(50) UNIQUE NOT NULL,
  password           VARCHAR(255) NOT NULL,
  pinecone_namespace VARCHAR(50),
  created_at         TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS vendors (
  vendor_id      SERIAL PRIMARY KEY,
  vendor_name    VARCHAR(100) NOT NULL,
  contact_person VARCHAR(100),
  phone          VARCHAR(20),
  email          VARCHAR(100),
  address        TEXT,
  category       VARCHAR(50),
  status         VARCHAR(10) DEFAULT 'Active'
                 CHECK (status IN ('Active','Inactive')),
  created_at     TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS documents (
  doc_id        SERIAL PRIMARY KEY,
  vendor_id     INT REFERENCES vendors(vendor_id),
  user_id       INT REFERENCES users(user_id),
  file_name     VARCHAR(200),
  doc_type      VARCHAR(20) CHECK (doc_type IN ('contract','invoice','policy','log')),
  file_size     VARCHAR(20),
  upload_date   TIMESTAMPTZ DEFAULT NOW(),
  pinecone_ids  TEXT
);

CREATE INDEX IF NOT EXISTS idx_vendors_status ON vendors(status);
CREATE INDEX IF NOT EXISTS idx_doc_user       ON documents(user_id);
CREATE INDEX IF NOT EXISTS idx_doc_vendor     ON documents(vendor_id);

INSERT INTO users(username, password, pinecone_namespace)
  VALUES ('admin','admin123','user_admin')
  ON CONFLICT(username) DO NOTHING;

INSERT INTO vendors(vendor_name,contact_person,phone,email,category,status) VALUES
  ('TechSupply Co.',    'Ravi Kumar',  '9876543210','ravi@techsupply.com', 'Electronics','Active'),
  ('OfficeWorld Ltd.',  'Sneha Patil', '9812345678','sneha@office.com',    'Stationery', 'Active'),
  ('FastLogistics Inc.','Arjun Mehta','9900112233','arjun@fastlog.com',   'Logistics',  'Active')
ON CONFLICT DO NOTHING;

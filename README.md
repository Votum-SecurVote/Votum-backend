# Votum-backend


```sql
CREATE DATABASE votum;
```

```sql
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
```

```sql
CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  full_name TEXT NOT NULL,
  email TEXT UNIQUE NOT NULL,
  phone TEXT UNIQUE NOT NULL,

  password_hash TEXT NOT NULL,
  aadhaar_hash TEXT UNIQUE NOT NULL,

  dob DATE NOT NULL,
  gender TEXT,
  address TEXT,

  status TEXT CHECK (status IN ('PENDING','APPROVED','REJECTED')) DEFAULT 'PENDING',

  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);
```
```sql
CREATE TABLE user_biometrics (
  user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,

  face_embedding BYTEA NOT NULL,
  photo_path TEXT,
  aadhaar_pdf_path TEXT
);
```
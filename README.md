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
  role VARCHAR(20) DEFAULT 'USER',

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

### Mac/Linux
```bash
export DB_PASSWORD=your_password
```

### Windows
```bash
set DB_PASSWORD=your_password
```

## Admin

```sql
CREATE TABLE admins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    role VARCHAR(20) DEFAULT 'ADMIN',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
```

```sql
INSERT INTO admins (
    full_name,
    email,
    password_hash,
    created_at,
    updated_at
)
VALUES (
    'System Admin',
    'admin@votum.com',
    '$2a$12$k2vnWiFuqvZ3EqKpFVMzIuK1nSUrL9sAFhFv.squlBuz5jYQfEKpy',
    NOW(),
    NOW()
);
```

## elections

```sql
CREATE TABLE elections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    description TEXT,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    status VARCHAR(20) DEFAULT 'DRAFT',
    created_at TIMESTAMP DEFAULT NOW()
);
```

```sql
CREATE TABLE ballots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    election_id UUID NOT NULL REFERENCES elections(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT,
    max_selections INTEGER DEFAULT 1,
    status VARCHAR(20) DEFAULT 'DRAFT',
    created_at TIMESTAMP DEFAULT NOW()
);
```

```sql
CREATE TABLE candidates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ballot_id UUID NOT NULL REFERENCES ballots(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    party TEXT,
    symbol TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);
```
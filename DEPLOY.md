# Deploy Aiven PostgreSQL + Render

## 1. Aiven

Create an **Aiven for PostgreSQL** service. In **Overview > Quick connect > Java**, copy Host, Port, Database, User and Password.

Render variables must use this format (the `jdbc:` prefix is required):

```text
DB_URL=jdbc:postgresql://HOST:PORT/defaultdb?sslmode=require
DB_USERNAME=avnadmin
DB_PASSWORD=AIVEN_PASSWORD
```

Do not run the SQL Server scripts in `database/` against Aiven. Flyway automatically applies `src/main/resources/db/migration/V1__create_schema.sql` on first startup.

## 2. GitHub

From the project directory:

```powershell
git init
git add .
git commit -m "Prepare Spring Boot app for Aiven and Render"
git branch -M main
git remote add origin https://github.com/YOUR_ACCOUNT/YOUR_REPOSITORY.git
git push -u origin main
```

The real `.env` is ignored and must never be pushed.

## 3. Render

Choose **New > Blueprint**, connect the GitHub repository, and select `render.yaml`. Enter the secret values requested by Render:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `BOOTSTRAP_ADMIN_PASSWORD` (strong password, used only when the users table is empty)
- `OPENAI_API_KEY`, `MAIL_USERNAME`, `MAIL_PASSWORD` only if those features are used

Render builds the Dockerfile, binds Spring Boot to its `PORT`, and checks `/health`. A healthy deployment returns `OK` only when PostgreSQL is reachable.

`render.yaml` declares a 1 GB persistent disk at `/app/uploads` so doctor images survive restarts. Render persistent disks require an eligible paid service plan; remove the `disk` block only if temporary uploads are acceptable.

The first account is `admin` (or `BOOTSTRAP_ADMIN_USERNAME`) with the bootstrap password. After a user already exists, changing the bootstrap variables does not overwrite database accounts.

## 4. Legacy SQL Server data

The application runtime is now PostgreSQL. SQL Server `.sql` files are retained only as migration sources and cannot be executed on Aiven. Export table data as CSV and import in dependency order:

1. `users`
2. `doctors`, `patients`
3. `medicalrecords`, `patientdailylogs`, `aiadvicehistory`, `devicereadings`, `healthalerts`, `nextappointment`
4. `healthindicators`, `aiwarnings`

After importing explicit identity IDs, synchronize each PostgreSQL identity sequence before creating new records.

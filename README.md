# CardDemo Java

A Spring Boot port of the AWS CardDemo COBOL mainframe application.

## Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 14+

## Getting Started

### 1. Configure the database

Create a PostgreSQL database and user:

```sql
CREATE DATABASE carddemo;
CREATE USER carddemo WITH PASSWORD '<your-password>';
GRANT ALL PRIVILEGES ON DATABASE carddemo TO carddemo;
```

### 2. Set environment variables

All secrets **must** be supplied via environment variables — never hardcoded. Copy
the template below and fill in real values before running the application:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/carddemo
export SPRING_DATASOURCE_USERNAME=carddemo
export SPRING_DATASOURCE_PASSWORD=<your-password>
export JWT_SECRET=<your-jwt-secret>
```

> ⚠️ **Never commit real credentials to source control.** See [SECURITY.md](SECURITY.md).

### 3. Build and run

```bash
mvn clean package -DskipTests
java -jar target/carddemo-*.jar
```

Or with Maven directly:

```bash
mvn spring-boot:run
```

The application starts on `http://localhost:8080`.

## Configuration Reference

| Environment Variable          | Description                          | Required |
|-------------------------------|--------------------------------------|----------|
| `SPRING_DATASOURCE_URL`       | JDBC URL for the PostgreSQL database | Yes      |
| `SPRING_DATASOURCE_USERNAME`  | Database username                    | Yes      |
| `SPRING_DATASOURCE_PASSWORD`  | Database password                    | Yes      |
| `JWT_SECRET`                  | Secret key used to sign JWT tokens   | Yes      |
| `JWT_EXPIRATION_MS`           | JWT token lifetime in milliseconds   | No       |

## Running Tests

```bash
mvn test
```

## Security

See [SECURITY.md](SECURITY.md) for the project's security policy and responsible
disclosure process.

## License

Apache 2.0

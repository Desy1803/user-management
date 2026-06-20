# User Management API

Una Spring Boot REST API per la gestione completa degli utenti con supporto per CRUD, ricerca avanzata e importazione da CSV.

## Requisiti

- **Java 21** o superiore
- **Docker** e **Docker Compose**
- **Maven** 3.8+ (opzionale se usi Docker)

## Struttura Progetto

```
user-management/
├── src/
│   ├── main/
│   │   ├── java/com/assignment/user_management/
│   │   │   ├── controller/    # REST API endpoints
│   │   │   ├── service/       # Business logic
│   │   │   ├── repository/    # Data access layer
│   │   │   └── entity/        # JPA entities
│   │   └── resources/
│   │       └── application.yml  # Spring configuration
│   └── test/
├── docker-compose.yml          # PostgreSQL + App services
├── Dockerfile                  # Application image
├── pom.xml                     # Maven configuration
└── mvnw                        # Maven wrapper
```

## Quick Start

### Opzione 1: Con Docker Compose (Consigliato)

```bash
# Clona il repository
git clone <repository-url>
cd user-management

# Avvia PostgreSQL e l'applicazione
docker-compose up -d

# Verifica che i container siano in esecuzione
docker ps
```

L'API sarà disponibile su: **http://localhost:8080/api/users**

L'applicazione genera automaticamente la documentazione Swagger:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs
- **OpenAPI YAML:** http://localhost:8080/v3/api-docs.yaml

### Opzione 2: Localmente con Maven

```bash
# Start PostgreSQL container
docker run --name postgres-db -e POSTGRES_PASSWORD=PasswordSQL123! \
  -e POSTGRES_DB=user_management_db -p 5432:5432 -d postgres:15-alpine

# Compila e avvia l'applicazione
./mvnw spring-boot:run
```

L'API sarà disponibile su: **http://localhost:8080/api/users**

L'applicazione genera automaticamente la documentazione Swagger:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs
- **OpenAPI YAML:** http://localhost:8080/v3/api-docs.yaml

## Configurazione

### Variabili d'Ambiente (Docker Compose)

Nel file `docker-compose.yml` puoi personalizzare:

```yaml
environment:
  POSTGRES_USER: postgres          # Username DB
  POSTGRES_PASSWORD: PasswordSQL123!  # Password DB
  POSTGRES_DB: user_management_db  # Nome database
```

### File di Configurazione Spring

Modifica `src/resources/application.yml` per altre configurazioni:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/user_management_db
    username: postgres
    password: PasswordSQL123!
```

## API Endpoints

### Gestione Utenti

#### Crea un utente
```bash
POST /api/users
Content-Type: application/json

{
  "firstName": "Mario",
  "lastName": "Rossi",
  "email": "mario.rossi@example.com",
  "address": "Via Roma 123, Roma"
}
```

#### Ottieni tutti gli utenti (con paginazione)
```bash
GET /api/users?page=0&size=10
```

#### Ricerca utenti per nome e/o cognome
```bash
# Per nome
GET /api/users?firstName=Mario&page=0&size=10

# Per cognome
GET /api/users?lastName=Rossi&page=0&size=10

# Per nome e cognome
GET /api/users?firstName=Mario&lastName=Rossi&page=0&size=10
```

#### Ottieni un utente per ID
```bash
GET /api/users/{id}
```

#### Aggiorna un utente
```bash
PUT /api/users/{id}
Content-Type: application/json

{
  "firstName": "Marco",
  "lastName": "Bianchi",
  "email": "marco.bianchi@example.com",
  "address": "Via Milano 456, Milano"
}
```

#### Elimina un utente
```bash
DELETE /api/users/{id}
```

### Importazione da CSV

#### Importa utenti da file CSV
```bash
POST /api/users/import
Content-Type: multipart/form-data

# Allegare il file CSV nel campo "file"
```

**Formato CSV:**
```csv
firstName,lastName,email,address
Mario,Rossi,mario.rossi@example.com,Via Roma 123
Luigi,Bianchi,luigi.bianchi@example.com,Via Milano 456
```

La prima riga (header) viene automaticamente ignorata se contiene "first" e "last".

## Schema Database

La tabella `users` viene creata automaticamente all'avvio:

```sql
CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  first_name VARCHAR(255),
  last_name VARCHAR(255),
  email VARCHAR(255) NOT NULL UNIQUE,
  address VARCHAR(255)
);
```

## Comandi Utili

### Docker

```bash
# Visualizza i log dell'applicazione
docker-compose logs -f app

# Visualizza i log di PostgreSQL
docker-compose logs -f postgres-db

# Stoppa tutti i container
docker-compose down

# Stoppa e elimina i volumi (pulizia totale)
docker-compose down -v

# Riavvia i container
docker-compose restart
```

### Maven

```bash
# Compila il progetto
./mvnw clean compile

# Esegui i test
./mvnw test

# Build del progetto (crea .jar)
./mvnw clean package

# Pulisci i file generati
./mvnw clean
```

## Troubleshooting

### Porta 5432 già in uso
```bash
# Cambia la porta nel docker-compose.yml o stoppa il container esistente
docker stop postgres-db
```

### Porta 8080 già in uso
```bash
# Cambia nel docker-compose.yml o stoppa il container dell'app
docker stop user_management_app
```

### Database non raggiungibile
Assicurati che PostgreSQL sia in esecuzione:
```bash
docker ps | grep postgres
```

### Errori di build Maven
```bash
# Pulisci la cache di Maven
./mvnw clean

# Rivedi i log di compilazione
./mvnw compile -X
```

## Tecnologie Utilizzate

- **Spring Boot 4.1.0** - Framework Web
- **Spring Data JPA** - ORM e Data Access
- **PostgreSQL 15** - Database
- **Lombok** - Riduzione boilerplate
- **Maven** - Build tool
- **Docker** - Containerizzazione

## Gestione dei Dati

- **Ricerca:** Supporta ricerca case-insensitive per nome e/o cognome
- **Paginazione:** Tutti gli endpoint LIST supportano paginazione via query params
- **Email unica:** Il campo email è obbligatorio e univoco nel database
- **Import CSV:** Supporta CSV con headers e quoted values



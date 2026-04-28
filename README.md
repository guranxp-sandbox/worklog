# Worklog

Personal time-tracking app.

## Running locally

### Start the database

```bash
docker-compose up -d
```

### Start the backend (dev profile)

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Backend runs at `http://localhost:8080`.

### Open the frontend

Open `frontend/index.html` directly in a browser, or serve it with any static file server:

```bash
cd frontend
python3 -m http.server 3000
```

Then visit `http://localhost:3000`.

> The frontend calls `/actuator/health` via a relative path, so it must be served from the same origin as the backend, or you need to proxy `/actuator` to `localhost:8080`.

<div align="center">
  <img src="frontend/src/assets/styles/icons/logo.png" alt="MelodyHub logo" width="220" />

  <p>A music platform for discovering, saving, and managing a personal music library.</p>

  <p>
    <img src="https://img.shields.io/badge/Vue.js-3-42B883?logo=vue.js&logoColor=white" alt="Vue.js" />
    <img src="https://img.shields.io/badge/Vite-6-646CFF?logo=vite&logoColor=white" alt="Vite" />
    <img src="https://img.shields.io/badge/Tailwind_CSS-4-06B6D4?logo=tailwindcss&logoColor=white" alt="Tailwind CSS" />
    <img src="https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white" alt="Java" />
    <img src="https://img.shields.io/badge/Jakarta_Servlet-6-1F6FEB?logo=jakarta&logoColor=white" alt="Jakarta Servlet" />
    <img src="https://img.shields.io/badge/MySQL-8.4-4479A1?logo=mysql&logoColor=white" alt="MySQL" />
    <img src="https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white" alt="Docker" />
    <img src="https://img.shields.io/badge/Vercel-ready-000000?logo=vercel&logoColor=white" alt="Vercel" />
  </p>
</div>

## About

MelodyHub is a music web application built with a Vue frontend and a Java Servlet backend. Users can:

- discover songs, albums, and artists;
- create playlists, like songs, and view listening history;
- register an account, manage a profile, and play music;
- apply as an artist and manage uploaded songs.

## Tech stack

- **Frontend:** Vue 3, Vite, Tailwind CSS, Pinia, Vue Router, and Axios.
- **Backend:** Java 17, Jakarta Servlet 6, JDBC, Jackson, and JWT.
- **Data and deployment:** MySQL 8.4, Docker Compose, ImageKit, and Vercel.

## Getting started

### Frontend

```bash
cd frontend
npm install
npm run dev
```

### Backend and MySQL

```bash
docker compose up --build
```

The frontend runs at the Vite URL shown in the terminal. The backend is available at `http://localhost:8080` by default.

## Project structure

```text
MelodyHub/
├── frontend/   # Vue 3 + Vite application
├── backend/    # Java Servlet API application
├── docs/       # Project documentation
└── docker-compose.yml
```

## Production build

```bash
cd frontend
npm run build

cd ../backend
mvn clean package
```

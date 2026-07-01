<script setup>
const songs = [
  {
    title: 'Midnight Drive',
    artist: 'Nova Lane',
    album: 'City Lights',
    status: 'Published'
  },
  {
    title: 'Blue Static',
    artist: 'The Echo Room',
    album: 'Signal',
    status: 'Draft'
  },
  {
    title: 'Golden Hour',
    artist: 'Mira Sol',
    album: 'Sunset Notes',
    status: 'Published'
  }
];
</script>

<template>
  <div class="app-shell min-vh-100">
    <nav class="navbar navbar-expand-lg bg-white border-bottom sticky-top">
      <div class="container">
        <a class="navbar-brand fw-bold text-primary" href="#">MelodyHub</a>
        <button
          class="navbar-toggler"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#mainNavbar"
          aria-controls="mainNavbar"
          aria-expanded="false"
          aria-label="Toggle navigation"
        >
          <span class="navbar-toggler-icon"></span>
        </button>

        <div id="mainNavbar" class="collapse navbar-collapse">
          <ul class="navbar-nav ms-auto mb-2 mb-lg-0">
            <li class="nav-item">
              <a class="nav-link active" href="#">Dashboard</a>
            </li>
            <li class="nav-item">
              <a class="nav-link" href="#">Songs</a>
            </li>
            <li class="nav-item">
              <a class="nav-link" href="#">Artists</a>
            </li>
          </ul>
        </div>
      </div>
    </nav>

    <main class="container py-4">
      <div class="d-flex flex-column flex-lg-row align-items-lg-center justify-content-between gap-3 mb-4">
        <div>
          <h1 class="h3 fw-bold mb-1">Music Dashboard</h1>
          <p class="text-secondary mb-0">Manage songs, artists, albums, and uploads for MelodyHub.</p>
        </div>
        <button class="btn btn-primary px-4" type="button">Add Song</button>
      </div>

      <div class="row g-3 mb-4">
        <div class="col-12 col-md-4">
          <section class="card stat-card h-100">
            <div class="card-body">
              <p class="text-secondary small mb-1">Songs</p>
              <p class="display-6 fw-bold mb-0">128</p>
            </div>
          </section>
        </div>
        <div class="col-12 col-md-4">
          <section class="card stat-card h-100">
            <div class="card-body">
              <p class="text-secondary small mb-1">Artists</p>
              <p class="display-6 fw-bold mb-0">42</p>
            </div>
          </section>
        </div>
        <div class="col-12 col-md-4">
          <section class="card stat-card h-100">
            <div class="card-body">
              <p class="text-secondary small mb-1">Albums</p>
              <p class="display-6 fw-bold mb-0">31</p>
            </div>
          </section>
        </div>
      </div>

      <section class="card">
        <div class="card-header bg-white d-flex flex-column flex-md-row gap-3 align-items-md-center justify-content-between">
          <h2 class="h5 mb-0">Recent Songs</h2>
          <input class="form-control search-input" type="search" placeholder="Search songs" aria-label="Search songs" />
        </div>
        <div class="table-responsive">
          <table class="table align-middle mb-0">
            <thead>
              <tr>
                <th scope="col">Title</th>
                <th scope="col">Artist</th>
                <th scope="col">Album</th>
                <th scope="col">Status</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="song in songs" :key="song.title">
                <td class="fw-semibold">{{ song.title }}</td>
                <td>{{ song.artist }}</td>
                <td>{{ song.album }}</td>
                <td>
                  <span
                    class="badge"
                    :class="song.status === 'Published' ? 'text-bg-success' : 'text-bg-warning'"
                  >
                    {{ song.status }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </main>
  </div>
</template>

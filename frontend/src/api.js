const BASE = import.meta.env.VITE_API_URL || '';

export const getToken = () => localStorage.getItem('token');
export const setToken = (t) => (t ? localStorage.setItem('token', t) : localStorage.removeItem('token'));

export const assetUrl = (url) => (url && url.startsWith('/') ? BASE + url : url);
export const streamUrl = (id) => `${BASE}/api/songs/${id}/stream?token=${encodeURIComponent(getToken() || '')}`;

async function request(path, { method = 'GET', body, form } = {}) {
  const headers = {};
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;
  let payload;
  if (form) payload = form;
  else if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    payload = JSON.stringify(body);
  }
  const res = await fetch(BASE + path, { method, headers, body: payload });
  if (res.status === 401 && token) {
    setToken(null);
    window.dispatchEvent(new Event('auth:logout'));
  }
  if (!res.ok) {
    let msg = `Request failed (${res.status})`;
    try {
      msg = (await res.json()).message || msg;
    } catch {
      /* no body */
    }
    throw new Error(msg);
  }
  if (res.status === 204) return null;
  return res.json();
}

const q = (params) => {
  const s = new URLSearchParams(Object.entries(params).filter(([, v]) => v !== undefined && v !== ''));
  return s.toString();
};

export const api = {
  register: (b) => request('/api/auth/register', { method: 'POST', body: b }),
  login: (b) => request('/api/auth/login', { method: 'POST', body: b }),
  me: () => request('/api/me'),
  updateMe: (b) => request('/api/me', { method: 'PUT', body: b }),

  songs: (params) => request(`/api/songs?${q(params)}`),
  played: (id) => request(`/api/songs/${id}/played`, { method: 'POST' }),
  createSong: (form) => request('/api/songs', { method: 'POST', form }),
  updateSong: (id, b) => request(`/api/songs/${id}`, { method: 'PUT', body: b }),
  deleteSong: (id) => request(`/api/songs/${id}`, { method: 'DELETE' }),

  artists: () => request('/api/artists'),
  artist: (id) => request(`/api/artists/${id}`),
  createArtist: (form) => request('/api/artists', { method: 'POST', form }),
  updateArtist: (id, form) => request(`/api/artists/${id}`, { method: 'PUT', form }),
  deleteArtist: (id) => request(`/api/artists/${id}`, { method: 'DELETE' }),
  albums: () => request('/api/albums'),
  album: (id) => request(`/api/albums/${id}`),
  createAlbum: (form) => request('/api/albums', { method: 'POST', form }),
  updateAlbum: (id, form) => request(`/api/albums/${id}`, { method: 'PUT', form }),
  deleteAlbum: (id) => request(`/api/albums/${id}`, { method: 'DELETE' }),

  playlists: () => request('/api/playlists'),
  playlist: (id) => request(`/api/playlists/${id}`),
  createPlaylist: (b) => request('/api/playlists', { method: 'POST', body: b }),
  updatePlaylist: (id, b) => request(`/api/playlists/${id}`, { method: 'PUT', body: b }),
  deletePlaylist: (id) => request(`/api/playlists/${id}`, { method: 'DELETE' }),
  addToPlaylist: (id, songId) => request(`/api/playlists/${id}/songs/${songId}`, { method: 'POST' }),
  removeFromPlaylist: (id, songId) => request(`/api/playlists/${id}/songs/${songId}`, { method: 'DELETE' }),

  like: (id) => request(`/api/songs/${id}/like`, { method: 'POST' }),
  unlike: (id) => request(`/api/songs/${id}/like`, { method: 'DELETE' }),
  liked: () => request('/api/me/liked'),

  history: (limit = 20) => request(`/api/history?limit=${limit}`),
  recordPlay: (songId) => request('/api/history', { method: 'POST', body: { songId } }),

  youtubeSearch: (query) => request(`/api/youtube/search?${q({ q: query })}`),
  youtubeSong: (videoId) => request('/api/youtube/songs', { method: 'POST', body: { videoId } }),
};

// YouTube search results have no DB id until saved. Returns a promise of a song with a real id
// (local songs and already-saved songs pass straight through); saves are de-duplicated per video.
const saving = new Map();
const savedIds = new Map();
export function resolveSong(song) {
  if (song.id != null) return Promise.resolve(song);
  if (!saving.has(song.externalId)) {
    const p = api
      .youtubeSong(song.externalId)
      .then((saved) => {
        savedIds.set(song.externalId, saved.id);
        return saved;
      })
      .catch((e) => {
        saving.delete(song.externalId);
        throw e;
      });
    saving.set(song.externalId, p);
  }
  return saving.get(song.externalId);
}

// Database id if known (without any network call), otherwise null.
export const knownId = (song) => song.id ?? savedIds.get(song.externalId) ?? null;

// Stable identity for lists/queues: YouTube songs may not have a database id yet.
export const songKey = (song) => (song.source === 'YOUTUBE' ? `yt:${song.externalId}` : `l:${song.id}`);

// Map a /api/youtube/search item to the song shape used by queue and lists.
export const youtubeResultToSong = (v) => ({
  id: v.songId ?? null,
  source: 'YOUTUBE',
  externalId: v.videoId,
  title: v.title,
  artistId: null,
  artistName: v.channelTitle,
  coverUrl: v.thumbnailUrl,
  thumbnailUrl: v.thumbnailUrl,
  durationSec: v.durationSec,
});


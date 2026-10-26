import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { api, knownId, resolveSong } from '../api';
import { useAuth } from './AuthContext';

const LibraryContext = createContext(null);
export const useLibrary = () => useContext(LibraryContext);

export function LibraryProvider({ children }) {
  const { user } = useAuth();
  const [playlists, setPlaylists] = useState([]);
  const [likedIds, setLikedIds] = useState(new Set());

  const refreshPlaylists = useCallback(() => api.playlists().then(setPlaylists).catch(() => {}), []);
  const refreshLiked = useCallback(
    () => api.liked().then((l) => setLikedIds(new Set(l.map((s) => s.id)))).catch(() => {}),
    [],
  );

  useEffect(() => {
    if (user) {
      refreshPlaylists();
      refreshLiked();
    } else {
      setPlaylists([]);
      setLikedIds(new Set());
    }
  }, [user, refreshPlaylists, refreshLiked]);

  const isLiked = (song) => {
    const id = knownId(song);
    return id != null && likedIds.has(id);
  };

  // Accepts a song object; YouTube results are saved first so they get a database id.
  const toggleLike = async (song) => {
    let songId;
    try {
      songId = (await resolveSong(song)).id;
    } catch {
      return;
    }
    const liked = likedIds.has(songId);
    setLikedIds((prev) => {
      const n = new Set(prev);
      if (liked) n.delete(songId);
      else n.add(songId);
      return n;
    });
    try {
      await (liked ? api.unlike(songId) : api.like(songId));
    } catch {
      refreshLiked();
    }
  };

  return (
    <LibraryContext.Provider value={{ playlists, likedIds, isLiked, refreshPlaylists, refreshLiked, toggleLike }}>
      {children}
    </LibraryContext.Provider>
  );
}

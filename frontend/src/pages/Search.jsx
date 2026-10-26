import { useEffect, useState } from 'react';
import { api, youtubeResultToSong } from '../api';
import SongList from '../components/SongList';

export default function Search() {
  const [text, setText] = useState('');
  const [results, setResults] = useState(null);
  const [error, setError] = useState('');
  const [yt, setYt] = useState(null); // null = not searched, [] = no results
  const [ytLoading, setYtLoading] = useState(false);
  const [ytError, setYtError] = useState('');

  useEffect(() => {
    const q = text.trim();
    if (!q) {
      setResults(null);
      return;
    }
    let stale = false;
    const t = setTimeout(() => {
      api.songs({ query: q, size: 50 })
        .then((r) => !stale && setResults(r.content))
        .catch((e) => !stale && setError(e.message));
    }, 250);
    return () => {
      stale = true;
      clearTimeout(t);
    };
  }, [text]);

  // YouTube searches cost API quota, so wait until typing pauses.
  useEffect(() => {
    const q = text.trim();
    setYtError('');
    if (q.length < 2) {
      setYt(null);
      setYtLoading(false);
      return;
    }
    let stale = false;
    setYtLoading(true);
    const t = setTimeout(() => {
      api.youtubeSearch(q)
        .then((r) => !stale && setYt(r.map(youtubeResultToSong)))
        .catch((e) => {
          if (stale) return;
          setYt(null);
          setYtError(e.message);
        })
        .finally(() => !stale && setYtLoading(false));
    }, 700);
    return () => {
      stale = true;
      clearTimeout(t);
    };
  }, [text]);

  return (
    <>
      <h1>Search</h1>
      <input
        className="search-input"
        type="search"
        autoFocus
        placeholder="Search by song, artist or album"
        aria-label="Search songs"
        value={text}
        onChange={(e) => setText(e.target.value)}
      />
      {error && <div className="error" role="alert">{error}</div>}
      {results && (
        <section aria-label="Your library results">
          <h2>Your library</h2>
          {results.length ? <SongList songs={results} /> : <p className="sub">No results for “{text}”.</p>}
        </section>
      )}
      {text.trim().length >= 2 && (
        <section aria-label="YouTube results">
          <h2>YouTube <span className="sub yt-powered">Powered by YouTube</span></h2>
          {ytLoading && <p className="sub">Searching YouTube…</p>}
          {ytError && <div className="error" role="alert">{ytError}</div>}
          {!ytLoading && yt && (yt.length ? <SongList songs={yt} /> : <p className="sub">No YouTube results.</p>)}
        </section>
      )}
    </>
  );
}

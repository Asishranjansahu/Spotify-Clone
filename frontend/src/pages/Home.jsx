import { useEffect, useState } from 'react';
import { api } from '../api';
import SongList from '../components/SongList';

const PAGE_SIZE = 20;

export default function Home() {
  const [data, setData] = useState({ content: [], totalPages: 0 });
  const [page, setPage] = useState(0);
  const [recent, setRecent] = useState([]);
  const [error, setError] = useState('');

  useEffect(() => {
    api.songs({ page, size: PAGE_SIZE }).then(setData).catch((e) => setError(e.message));
  }, [page]);

  useEffect(() => {
    api.history(30).then((h) => {
      const seen = new Set();
      setRecent(h.map((x) => x.song).filter((s) => !seen.has(s.id) && seen.add(s.id)).slice(0, 8));
    }).catch(() => {});
  }, []);

  return (
    <>
      <h1>Home</h1>
      {error && <div className="error" role="alert">{error}</div>}
      {recent.length > 0 && (
        <section>
          <h2>Recently played</h2>
          <SongList songs={recent} />
        </section>
      )}
      <section>
        <h2>All songs</h2>
        <SongList songs={data.content} />
        {data.totalPages > 1 && (
          <div className="pager">
            <button className="btn" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button>
            <span>Page {page + 1} of {data.totalPages}</span>
            <button className="btn" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>Next</button>
          </div>
        )}
      </section>
    </>
  );
}

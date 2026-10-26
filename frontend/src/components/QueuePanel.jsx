import { assetUrl, songKey } from '../api';
import { usePlayer } from '../context/PlayerContext';

export default function QueuePanel() {
  const p = usePlayer();
  if (!p.queueOpen) return null;
  return (
    <aside className="queue" aria-label="Queue">
      <div className="queue-head">
        <h3>Queue</h3>
        <button className="icon-btn" aria-label="Close queue" onClick={p.toggleQueue}>✕</button>
      </div>
      {p.queue.length === 0 && <p className="sub">Queue is empty.</p>}
      <ol>
        {p.queue.map((s, i) => (
          <li key={`${songKey(s)}-${i}`} className={i === p.index ? 'active' : ''}>
            <button className="queue-item" onClick={() => p.playFromQueue(i)}>
              {s.coverUrl ? <img className="cover xs" src={assetUrl(s.coverUrl)} alt="" /> : <div className="cover xs placeholder">♪</div>}
              <span className="meta">
                <span className="title">{s.title}</span>
                <span className="sub">{s.artistName}</span>
              </span>
            </button>
            {i !== p.index && (
              <button className="icon-btn" aria-label={`Remove ${s.title} from queue`} onClick={() => p.removeFromQueue(i)}>✕</button>
            )}
          </li>
        ))}
      </ol>
    </aside>
  );
}

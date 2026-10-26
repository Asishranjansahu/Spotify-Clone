import { Outlet } from 'react-router-dom';
import { LibraryProvider } from '../context/LibraryContext';
import { PlayerProvider } from '../context/PlayerContext';
import Sidebar from './Sidebar';
import PlayerBar from './PlayerBar';
import QueuePanel from './QueuePanel';

// Layout stays mounted while routes change, so the player keeps playing across navigation.
export default function Layout() {
  return (
    <LibraryProvider>
      <PlayerProvider>
        <div className="app">
          <Sidebar />
          <main className="main">
            <Outlet />
          </main>
          <QueuePanel />
          <PlayerBar />
        </div>
      </PlayerProvider>
    </LibraryProvider>
  );
}

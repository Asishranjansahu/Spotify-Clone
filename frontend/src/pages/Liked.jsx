import { useEffect, useState } from 'react';
import { useLibrary } from '../context/LibraryContext';
import { api } from '../api';
import SongList from '../components/SongList';

export default function Liked() {
  const { likedIds } = useLibrary();
  const [songs, setSongs] = useState([]);

  useEffect(() => {
    api.liked().then(setSongs).catch(() => {});
    // refetch when the like set changes (e.g. unliking from the player bar)
  }, [likedIds]);

  return (
    <>
      <h1>Liked Songs</h1>
      <p className="sub">{songs.length} songs</p>
      <SongList songs={songs} />
    </>
  );
}

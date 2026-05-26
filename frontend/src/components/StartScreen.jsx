import { useState } from 'react'
import { startGame } from '../api/gameApi.js'

export default function StartScreen({ onStart }) {
  const [name, setName] = useState('')
  const [loading, setLoading] = useState(false)

  const handleStart = async () => {
    if (!name.trim()) return
    setLoading(true)
    const data = await startGame(name.trim())
    onStart(data)
  }

  return (
    <div className="start-screen">
      <div className="start-box">
        <div className="start-logo">KUBERNAUTS</div>
        <div className="start-sub">A Kubernetes Learning Experience</div>
        <div className="start-klink">
          "Welcome, recruit. I'm KLINK. Try not to crash anything."
        </div>
        <input
          className="start-input"
          placeholder="Enter your name, Kubernaut..."
          value={name}
          onChange={e => setName(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && handleStart()}
          autoFocus
        />
        <button className="start-btn" onClick={handleStart} disabled={loading || !name.trim()}>
          {loading ? 'INITIALISING...' : '▶ LAUNCH MISSION'}
        </button>
      </div>
    </div>
  )
}

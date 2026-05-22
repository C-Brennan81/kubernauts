import { useEffect, useState } from 'react'

export default function LevelBanner({ level, onDone }) {
  const [visible, setVisible] = useState(true)

  useEffect(() => {
    const t = setTimeout(() => {
      setVisible(false)
      setTimeout(onDone, 400)
    }, 3000)
    return () => clearTimeout(t)
  }, [onDone])

  const labels = { 1: 'BASIC OPS', 2: 'DEPLOYMENTS', 3: 'ADVANCED' }

  return (
    <div className={`level-banner ${visible ? 'banner-in' : 'banner-out'}`}>
      <div className="banner-inner">
        <div className="banner-level">LEVEL {level}</div>
        <div className="banner-label">{labels[level] ?? 'UNLOCKED'}</div>
        <div className="banner-sub">New missions available</div>
      </div>
    </div>
  )
}

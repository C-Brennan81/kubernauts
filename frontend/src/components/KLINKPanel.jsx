import { useEffect, useRef, useState } from 'react'

export default function KLINKPanel({ messages }) {
  const [displayed, setDisplayed] = useState('')
  const [typing, setTyping] = useState(false)
  const bottomRef = useRef(null)
  const timerRef = useRef(null)

  const latest = messages[messages.length - 1] ?? null
  const latestText = latest?.text ?? ''

  useEffect(() => {
    if (!latestText) return
    clearInterval(timerRef.current)
    setTyping(true)
    let i = 0
    setDisplayed('')
    timerRef.current = setInterval(() => {
      i++
      setDisplayed(latestText.slice(0, i))
      if (i >= latestText.length) {
        clearInterval(timerRef.current)
        setTyping(false)
      }
    }, 16)
    return () => clearInterval(timerRef.current)
  }, [latestText])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [displayed])

  return (
    <div className="klink-panel">
      <div className="klink-header">
        <span className="klink-badge">KLINK</span>
        <span className="klink-subtitle">Kubernetes Learning INtelligence Kernel</span>
        {typing && <span className="klink-cursor">▋</span>}
      </div>
      <div className="klink-body">
        {messages.slice(0, -1).map((m, i) => (
          <div key={i} className={`klink-old klink-type-${m.type ?? 'mission'}`}>
            <pre>{m.text}</pre>
          </div>
        ))}
        {latest && (
          <div className={`klink-current klink-type-${latest.type ?? 'mission'}`}>
            <pre>{displayed}</pre>
          </div>
        )}
        <div ref={bottomRef} />
      </div>
    </div>
  )
}

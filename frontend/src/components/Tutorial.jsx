import { useState } from 'react'

const SLIDES = [
  {
    title: 'Welcome to KUBERNAUTS',
    icon: '🚀',
    body: `You are the new station operator. The ship's AI — KLINK — will brief you on failures as they occur.

Your job: keep the station running by diagnosing and fixing problems.

Everything you do here maps to a real Kubernetes command. You're learning kubectl without knowing it.`,
  },
  {
    title: 'How Commands Work',
    icon: '⌨️',
    body: `Start with real kubectl syntax — proxy shortcuts are locked until you've used the real command once.

  kubectl get pods -l app=crew-quarters
  kubectl describe pod <name>
  kubectl logs <name>
  kubectl delete pod <name>

Once used, KLINK unlocks the shorter game syntax. Type "help" for the full reference, or open the PDA.`,
  },
  {
    title: 'Reading the Screen',
    icon: '🖥️',
    body: `TERMINAL (right) — your command input and output lives here.

KLINK (bottom left) — the ship AI reacts to your actions here. Not the same as terminal output.

SHIP MAP (top left) — live pod status. Green = running, yellow = warning, red = fault.

OBJECTIVES button — always shows exactly what's blocking your progress.

MULTIPLIER — solve scenarios fast for bonus points. It decays after 60s.`,
  },
]

const SEEN_KEY = 'kubernauts_tutorial_seen'

export default function Tutorial({ onDone }) {
  const [slide, setSlide] = useState(0)

  const next = () => {
    if (slide < SLIDES.length - 1) setSlide(s => s + 1)
    else {
      localStorage.setItem(SEEN_KEY, '1')
      onDone()
    }
  }

  const skip = () => {
    localStorage.setItem(SEEN_KEY, '1')
    onDone()
  }

  const s = SLIDES[slide]

  return (
    <div className="tutorial-overlay">
      <div className="tutorial-panel">
        <div className="tutorial-icon">{s.icon}</div>
        <div className="tutorial-title">{s.title}</div>
        <pre className="tutorial-body">{s.body}</pre>
        <div className="tutorial-dots">
          {SLIDES.map((_, i) => (
            <span key={i} className={`tutorial-dot ${i === slide ? 'active' : ''}`} />
          ))}
        </div>
        <div className="tutorial-actions">
          <button className="tutorial-btn secondary" onClick={skip}>Skip</button>
          <button className="tutorial-btn primary" onClick={next}>
            {slide < SLIDES.length - 1 ? 'Next →' : 'Start Mission'}
          </button>
        </div>
      </div>
    </div>
  )
}

export function shouldShowTutorial() {
  return !localStorage.getItem(SEEN_KEY)
}

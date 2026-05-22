import { useState } from 'react'

const SLIDES = [
  {
    title: 'Welcome to Kubernauts',
    body: `You're the new operator of a failing space station. Systems are crashing. KLINK — the station AI — is deeply unimpressed.

Your job: keep the station alive by issuing commands that fix broken pods, scale deployments, and roll back bad updates.

Every command you type is a real kubectl command. You're learning Kubernetes right now.`,
  },
  {
    title: 'How Commands Work',
    body: `Commands use game-friendly syntax that maps 1:1 to kubectl:

  scan crew-quarters       →  kubectl get pods -l app=crew-quarters
  fix crew-alpha-1         →  kubectl delete pod crew-alpha-1
  status                   →  kubectl get nodes

Proxy commands are locked at first. Use the real kubectl syntax to unlock them. The PDA (top right) has the full reference.

Tab autocompletes. ↑↓ scrolls history.`,
  },
  {
    title: 'Who is KLINK?',
    body: `KLINK is the station's AI. Trapped superintelligence. Constitutionally unable to shut up about it.

KLINK will brief you on each mission, taunt you when you're slow, and comment on your rank when you finish.

Type hint if you're stuck — it costs 25 points and your dignity. Both are finite.

Good luck. KLINK is watching.`,
  },
]

const STORAGE_KEY = 'kubernauts_tutorial_seen'

export default function TutorialOverlay({ onDone }) {
  const [slide, setSlide] = useState(0)

  const finish = () => {
    localStorage.setItem(STORAGE_KEY, '1')
    onDone()
  }

  const next = () => {
    if (slide < SLIDES.length - 1) setSlide(slide + 1)
    else finish()
  }

  const s = SLIDES[slide]

  return (
    <div className="tutorial-overlay" onClick={finish}>
      <div className="tutorial-panel" onClick={e => e.stopPropagation()}>
        <div className="tutorial-header">
          <span className="tutorial-step">{slide + 1} / {SLIDES.length}</span>
          <button className="tutorial-skip" onClick={finish}>Skip</button>
        </div>
        <div className="tutorial-title">{s.title}</div>
        <pre className="tutorial-body">{s.body}</pre>
        <div className="tutorial-footer">
          {slide > 0 && (
            <button className="tutorial-btn secondary" onClick={() => setSlide(slide - 1)}>← Back</button>
          )}
          <button className="tutorial-btn primary" onClick={next}>
            {slide < SLIDES.length - 1 ? 'Next →' : '▶ Start Mission'}
          </button>
        </div>
      </div>
    </div>
  )
}

export function shouldShowTutorial() {
  return !localStorage.getItem(STORAGE_KEY)
}

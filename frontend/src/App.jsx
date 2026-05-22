import { useState } from 'react'
import GameScreen from './components/GameScreen.jsx'
import StartScreen from './components/StartScreen.jsx'
import TutorialOverlay, { shouldShowTutorial } from './components/TutorialOverlay.jsx'

export default function App() {
  const [session, setSession] = useState(null)
  const [showTutorial, setShowTutorial] = useState(shouldShowTutorial)

  return (
    <>
      {session
        ? <GameScreen session={session} />
        : <StartScreen onStart={setSession} />}
      {showTutorial && <TutorialOverlay onDone={() => setShowTutorial(false)} />}
    </>
  )
}

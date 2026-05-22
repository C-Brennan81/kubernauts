import { useState } from 'react'
import GameScreen from './components/GameScreen.jsx'
import StartScreen from './components/StartScreen.jsx'

export default function App() {
  const [session, setSession] = useState(null)

  return session
    ? <GameScreen session={session} />
    : <StartScreen onStart={setSession} />
}

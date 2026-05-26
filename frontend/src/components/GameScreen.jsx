import { useEffect, useRef, useState } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import Terminal from './Terminal.jsx'
import ShipMap from './ShipMap.jsx'
import KLINKPanel from './KLINKPanel.jsx'
import PDA from './PDA.jsx'
import LevelBanner from './LevelBanner.jsx'
import { sendCommand, getIdleComment, getLeaderboard } from '../api/gameApi.js'
import ObjectivesPanel from './ObjectivesPanel.jsx'
import Tutorial, { shouldShowTutorial } from './Tutorial.jsx'

const ALERT_COLOR = { GREEN: '#00ff88', YELLOW: '#ffd700', RED: '#ff4444' }
const MULTIPLIER_COLOR = { 3: '#00ff88', 2: '#ffd700', 1: '#ff4444' }

export default function GameScreen({ session }) {
  const { sessionId, klinkMessage, state: initialState } = session
  const [stationState, setStationState] = useState(initialState)
  const [klinkMessages, setKlinkMessages] = useState(
    klinkMessage ? [{ text: klinkMessage, type: 'mission' }] : []
  )
  const [pdaOpen, setPdaOpen] = useState(false)
  const [levelBanner, setLevelBanner] = useState(null)
  const [leaderboard, setLeaderboard] = useState(null)
  const [showTutorial, setShowTutorial] = useState(shouldShowTutorial)
  const stompRef = useRef(null)
  const idleTimerRef = useRef(null)

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      onConnect: () => {
        client.subscribe(`/topic/klink/${sessionId}`, msg => {
          try {
            const payload = JSON.parse(msg.body)
            setKlinkMessages(prev => [...prev, { text: payload.message, type: payload.type }])
          } catch {
            setKlinkMessages(prev => [...prev, { text: msg.body, type: 'mission' }])
          }
        })
        client.subscribe(`/topic/levelup/${sessionId}`, msg => {
          setLevelBanner(parseInt(msg.body))
        })
        client.subscribe(`/topic/state/${sessionId}`, msg => {
          try { setStationState(JSON.parse(msg.body)) } catch {}
        })
        client.subscribe(`/topic/incident/${sessionId}`, msg => {
          if (msg.body === 'ended') setStationState(s => ({ ...s, incidentMode: false }))
        })
      }
    })
    client.activate()
    stompRef.current = client
    return () => client.deactivate()
  }, [sessionId])

  useEffect(() => {
    const schedule = () => {
      const delay = 45000 + Math.random() * 45000
      return setTimeout(async () => {
        const msg = await getIdleComment()
        setKlinkMessages(prev => [...prev, { text: msg, type: 'idle' }])
        idleTimerRef.current = schedule()
      }, delay)
    }
    idleTimerRef.current = schedule()
    return () => clearTimeout(idleTimerRef.current)
  }, [])

  const handleCommand = async (cmd) => {
    try {
      const result = await sendCommand(sessionId, cmd)
      if (result.state) {
        setStationState(result.state)
        if (result.scenarioComplete && result.state.currentScenario >= 20) {
          getLeaderboard().then(setLeaderboard)
        }
      }
      return result.output ?? '(no response)'
    } catch (err) {
      return `Error: ${err.message} — is the backend running?`
    }
  }

  const alert      = stationState?.alertLevel ?? 'GREEN'
  const level      = stationState?.currentLevel ?? 1
  const score      = stationState?.score ?? 0
  const scenario   = (stationState?.currentScenario ?? 0) + 1
  const multiplier = stationState?.scoreMultiplier ?? 3
  const incident   = stationState?.incidentMode ?? false
  const survival   = stationState?.survivalScore ?? 0

  return (
    <div className={`game-screen ${incident ? 'incident-active' : ''}`}>
      <div className="hud-bar">
        <div className="hud-title">KUBERNAUTS</div>
        <div className="hud-stats">
          <span className="hud-stat">
            ALERT: <span style={{ color: ALERT_COLOR[alert] }}>■ {alert}</span>
          </span>
          {!incident && <>
            <span className="hud-stat">LVL: <span className="hud-val">{level}</span></span>
            <span className="hud-stat">MISSION: <span className="hud-val">{Math.min(scenario, 20)}/20</span></span>
            <span className="hud-stat">
              MULTIPLIER: <span style={{ color: MULTIPLIER_COLOR[multiplier] }}>{multiplier}x</span>
            </span>
          </>}
          {incident && <>
            <span className="hud-stat incident-label">⚠ INCIDENT MODE</span>
            <span className="hud-stat">SURVIVAL: <span className="hud-val">{survival}</span></span>
          </>}
          <span className="hud-stat">SCORE: <span className="hud-val">{score}</span></span>
        </div>
        <button className="pda-btn" onClick={() => setPdaOpen(true)}>📟 PDA</button>
      </div>

      <div className="game-body">
        <div className="left-panel">
          <ShipMap state={stationState} />
          <ObjectivesPanel
            sessionId={sessionId}
            currentScenario={stationState?.currentScenario}
            incidentMode={incident}
          />
          <KLINKPanel messages={klinkMessages} />
        </div>
        <div className="right-panel">
          <Terminal onCommand={handleCommand} />
        </div>
      </div>

      {pdaOpen && <PDA onClose={() => setPdaOpen(false)} />}
      {levelBanner && <LevelBanner level={levelBanner} onDone={() => setLevelBanner(null)} />}
      {showTutorial && <Tutorial onDone={() => setShowTutorial(false)} />}
      {leaderboard && (
        <div className="leaderboard-overlay" onClick={() => setLeaderboard(null)}>
          <div className="leaderboard-panel" onClick={e => e.stopPropagation()}>
            <div className="leaderboard-title">CAMPAIGN COMPLETE — TOP SCORES</div>
            <table className="leaderboard-table">
              <thead>
                <tr><th>#</th><th>OPERATOR</th><th>SCORE</th></tr>
              </thead>
              <tbody>
                {leaderboard.map((e, i) => (
                  <tr key={e.id} className={e.playerName === stationState?.playerName ? 'lb-highlight' : ''}>
                    <td>{i + 1}</td>
                    <td>{e.playerName}</td>
                    <td>{e.score}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <button className="tutorial-btn primary" style={{ marginTop: 16 }} onClick={() => setLeaderboard(null)}>
              Continue to Incident Mode
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

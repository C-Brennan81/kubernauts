import { useEffect, useState } from 'react'
import { getObjectives } from '../api/gameApi.js'

export default function ObjectivesPanel({ sessionId, currentScenario, incidentMode }) {
  const [data, setData] = useState(null)

  useEffect(() => {
    if (incidentMode) return
    getObjectives(sessionId).then(setData)
  }, [sessionId, currentScenario, incidentMode])

  if (incidentMode) return (
    <div className="objectives-panel">
      <div className="obj-panel-header">⚠ INCIDENT MODE</div>
      <div className="obj-panel-body">
        <p className="obj-incident-text">Keep the station alive. Fix failures as they appear. Station goes RED for 60s = game over.</p>
      </div>
    </div>
  )

  if (!data) return null

  return (
    <div className="objectives-panel">
      <div className="obj-panel-header">
        <span>MISSION {data.level ? `L${data.level} — ` : ''}{data.title}</span>
      </div>
      <div className="obj-panel-body">
        <div className={`obj-step ${data.discoveryDone ? 'done' : 'active'}`}>
          <span className="obj-step-icon">{data.discoveryDone ? '✓' : '▶'}</span>
          <span>
            {data.discoveryDone
              ? 'Diagnostic complete'
              : <>Run: <code>{data.discoveryRequired || 'status'}</code></>}
          </span>
        </div>
        <div className={`obj-step ${!data.blocked ? 'done' : data.discoveryDone ? 'active' : 'locked'}`}>
          <span className="obj-step-icon">{!data.blocked ? '✓' : data.discoveryDone ? '▶' : '○'}</span>
          <span className="obj-step-hint">{data.objective}</span>
        </div>
        {data.blocked && data.discoveryDone && (
          <div className="obj-blocked-msg">{data.blockedReason}</div>
        )}
      </div>
    </div>
  )
}

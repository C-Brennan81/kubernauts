import { useState } from 'react'
import { getObjectives } from '../api/gameApi.js'

export default function ObjectivesButton({ sessionId }) {
  const [data, setData] = useState(null)
  const [open, setOpen] = useState(false)

  const toggle = async () => {
    if (!open) {
      const obj = await getObjectives(sessionId)
      setData(obj)
    }
    setOpen(o => !o)
  }

  return (
    <>
      <button className="obj-btn" onClick={toggle}>
        {open ? '✕ CLOSE' : '📋 OBJECTIVES'}
      </button>

      {open && data && (
        <div className="obj-panel">
          <div className="obj-header">
            <span className="obj-title">{data.title}</span>
            <span className="obj-level">LVL {data.level}</span>
          </div>

          <div className="obj-row">
            <span className={`obj-check ${data.discoveryDone ? 'done' : 'pending'}`}>
              {data.discoveryDone ? '✓' : '○'}
            </span>
            <span className="obj-label">
              {data.discoveryDone
                ? 'Diagnostic complete'
                : `Run diagnostic first: ${data.discoveryRequired}`}
            </span>
          </div>

          <div className="obj-row">
            <span className={`obj-check ${data.blocked ? 'pending' : 'done'}`}>
              {data.blocked ? '○' : '✓'}
            </span>
            <span className="obj-label">
              {data.blocked
                ? <span className="obj-blocked">{data.blockedReason}</span>
                : 'Ready — complete the objective below'}
            </span>
          </div>

          <div className="obj-objective">{data.objective}</div>
        </div>
      )}
    </>
  )
}

const STATUS_ICON = {
  RUNNING:    { icon: '●', cls: 'status-ok'   },
  PENDING:    { icon: '◌', cls: 'status-warn' },
  CRASH_LOOP: { icon: '✖', cls: 'status-err'  },
  OOM_KILLED: { icon: '✖', cls: 'status-err'  },
  TERMINATED: { icon: '✖', cls: 'status-err'  },
  UNKNOWN:    { icon: '?', cls: 'status-warn' },
}

const ALERT_COLOR = { GREEN: '#00ff88', YELLOW: '#ffd700', RED: '#ff4444' }

export default function StationMap({ state }) {
  if (!state) return null
  const { nodes = [], pods = [], alertLevel, score, currentScenario } = state

  const podsByNode = nodes.reduce((acc, n) => {
    acc[n.name] = pods.filter(p => p.nodeName === n.name)
    return acc
  }, {})

  return (
    <div className="station-map">
      <div className="map-header">
        <span>STATION ATLAS</span>
        <span style={{ color: ALERT_COLOR[alertLevel] }}>● {alertLevel}</span>
        <span>SCORE: {score}</span>
        <span>MISSION: {currentScenario + 1}/5</span>
      </div>

      <div className="modules-grid">
        {nodes.map(node => {
          const nodePods = podsByNode[node.name] || []
          const hasError = nodePods.some(p => p.status !== 'RUNNING')
          return (
            <div key={node.name} className={`module-card ${hasError ? 'module-warn' : ''}`}>
              <div className="module-name">
                {node.ready ? '▣' : '▢'} {node.name.toUpperCase()}
              </div>
              <div className="pod-list">
                {nodePods.map(pod => {
                  const { icon, cls } = STATUS_ICON[pod.status] || STATUS_ICON.UNKNOWN
                  return (
                    <div key={pod.name} className={`pod-row ${cls}`}>
                      <span>{icon}</span>
                      <span className="pod-name">{pod.name}</span>
                      {pod.restartCount > 0 && (
                        <span className="restart-badge">{pod.restartCount}↺</span>
                      )}
                    </div>
                  )
                })}
                {nodePods.length === 0 && (
                  <div className="pod-row status-dim">— empty —</div>
                )}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}

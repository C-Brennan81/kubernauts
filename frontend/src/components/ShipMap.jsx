const STATUS_COLOR = {
  RUNNING:    '#00ff88',
  PENDING:    '#ffd700',
  CRASH_LOOP: '#ff4444',
  OOM_KILLED: '#ff4444',
  TERMINATED: '#ff6666',
  UNKNOWN:    '#ffd700',
}

const STATUS_ICON = {
  RUNNING:    '●',
  PENDING:    '◐',
  CRASH_LOOP: '✖',
  OOM_KILLED: '✖',
  TERMINATED: '✕',
  UNKNOWN:    '?',
}

const ALERT_BORDER = { GREEN: '#00ff88', YELLOW: '#ffd700', RED: '#ff4444' }

export default function ShipMap({ state }) {
  if (!state) return null
  const { nodes = [], pods = [], alertLevel = 'GREEN', currentLevel = 1 } = state

  const podsByNode = nodes.reduce((acc, n) => {
    acc[n.name] = pods.filter(p => p.nodeName === n.name)
    return acc
  }, {})

  return (
    <div className="ship-map" style={{ borderBottomColor: ALERT_BORDER[alertLevel] }}>
      <div className="ship-map-header">
        <span className="ship-map-title">STATION ATLAS</span>
        <span className="ship-map-level">LVL {currentLevel}</span>
        <span className="ship-map-alert" style={{ color: ALERT_BORDER[alertLevel] }}>
          ■ {alertLevel}
        </span>
      </div>

      <div className="ship-nodes">
        {nodes.map(node => {
          const nodePods = podsByNode[node.name] || []
          const nodeAlert = nodePods.some(p => p.status !== 'RUNNING')
          return (
            <div key={node.name} className={`ship-node ${nodeAlert ? 'node-alert' : ''}`}>
              <div className="ship-node-name">
                <span className="ship-node-icon">{node.ready ? '▣' : '▢'}</span>
                {node.name.replace('module-', '').toUpperCase()}
              </div>
              <div className="ship-node-pods">
                {nodePods.length === 0
                  ? <span className="pod-empty">— empty —</span>
                  : nodePods.map(pod => (
                    <div key={pod.name} className="ship-pod">
                      <span className="ship-pod-icon" style={{ color: STATUS_COLOR[pod.status] ?? '#5a7a9a' }}>
                        {STATUS_ICON[pod.status] ?? '?'}
                      </span>
                      <span className="ship-pod-name">{pod.name}</span>
                      {pod.restartCount > 0 && (
                        <span className="ship-pod-restarts">{pod.restartCount}↺</span>
                      )}
                      <span className="ship-pod-status" style={{ color: STATUS_COLOR[pod.status] ?? '#5a7a9a' }}>
                        {pod.status?.replace('_', ' ')}
                      </span>
                    </div>
                  ))
                }
              </div>
            </div>
          )
        })}
      </div>

      <div className="ship-legend">
        <span style={{ color: '#00ff88' }}>● RUNNING</span>
        <span style={{ color: '#ffd700' }}>◐ PENDING</span>
        <span style={{ color: '#ff4444' }}>✖ FAULT</span>
      </div>
    </div>
  )
}

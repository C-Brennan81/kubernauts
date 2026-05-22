// Ship map: SVG visual of the station that evolves with level
// Level 1: core + 1 module lit
// Level 2: + 2 more modules
// Level 3: full ship with all connections

const STATUS_COLOR = {
  RUNNING:    '#00ff88',
  PENDING:    '#ffd700',
  CRASH_LOOP: '#ff4444',
  OOM_KILLED: '#ff4444',
  TERMINATED: '#ff4444',
  UNKNOWN:    '#ffd700',
}

export default function ShipMap({ state }) {
  if (!state) return null
  const { nodes = [], pods = [], currentLevel = 1 } = state

  const nodeStatus = nodes.reduce((acc, n) => {
    const nodePods = pods.filter(p => p.nodeName === n.name)
    const hasError = nodePods.some(p => p.status !== 'RUNNING')
    acc[n.name] = hasError ? 'error' : 'ok'
    return acc
  }, {})

  const nodeColor = (name) => {
    if (nodeStatus[name] === 'error') return '#ff4444'
    return '#00ff88'
  }

  const moduleOpacity = (requiredLevel) => currentLevel >= requiredLevel ? 1 : 0.2

  return (
    <div className="ship-map">
      <div className="ship-map-header">
        <span>SHIP SCHEMATIC</span>
        <span className="level-badge">LVL {currentLevel}</span>
      </div>
      <svg viewBox="0 0 300 200" className="ship-svg">
        {/* Ship hull outline */}
        <ellipse cx="150" cy="100" rx="130" ry="70" fill="none" stroke="#1e3a5f" strokeWidth="1.5" />

        {/* Core — always visible */}
        <circle cx="150" cy="100" r="18" fill="#0d1224" stroke="#4fc3f7" strokeWidth="2" />
        <text x="150" y="104" textAnchor="middle" fill="#4fc3f7" fontSize="7">CORE</text>

        {/* module-alpha — level 1 */}
        <g opacity={moduleOpacity(1)}>
          <line x1="150" y1="82" x2="150" y2="52" stroke="#1e3a5f" strokeWidth="1.5" strokeDasharray={currentLevel >= 1 ? "none" : "4,2"} />
          <circle cx="150" cy="42" r="14" fill="#0d1224" stroke={nodeColor('module-alpha')} strokeWidth="2" />
          <text x="150" y="40" textAnchor="middle" fill={nodeColor('module-alpha')} fontSize="5.5">module</text>
          <text x="150" y="48" textAnchor="middle" fill={nodeColor('module-alpha')} fontSize="5.5">alpha</text>
          {/* Pod dots */}
          {pods.filter(p => p.nodeName === 'module-alpha').map((pod, i) => (
            <circle key={pod.name} cx={138 + i * 8} cy="62" r="3"
              fill={STATUS_COLOR[pod.status] || '#5a7a9a'} />
          ))}
        </g>

        {/* module-beta — level 1 */}
        <g opacity={moduleOpacity(1)}>
          <line x1="133" y1="110" x2="90" y2="135" stroke="#1e3a5f" strokeWidth="1.5" />
          <circle cx="78" cy="142" r="14" fill="#0d1224" stroke={nodeColor('module-beta')} strokeWidth="2" />
          <text x="78" y="140" textAnchor="middle" fill={nodeColor('module-beta')} fontSize="5.5">module</text>
          <text x="78" y="148" textAnchor="middle" fill={nodeColor('module-beta')} fontSize="5.5">beta</text>
          {pods.filter(p => p.nodeName === 'module-beta').map((pod, i) => (
            <circle key={pod.name} cx={66 + i * 8} cy="125" r="3"
              fill={STATUS_COLOR[pod.status] || '#5a7a9a'} />
          ))}
        </g>

        {/* module-gamma — level 2 */}
        <g opacity={moduleOpacity(2)}>
          <line x1="167" y1="110" x2="210" y2="135" stroke="#1e3a5f" strokeWidth="1.5" />
          <circle cx="222" cy="142" r="14" fill="#0d1224" stroke={currentLevel >= 2 ? '#00ff88' : '#1e3a5f'} strokeWidth="2" />
          <text x="222" y="140" textAnchor="middle" fill={currentLevel >= 2 ? '#00ff88' : '#5a7a9a'} fontSize="5.5">module</text>
          <text x="222" y="148" textAnchor="middle" fill={currentLevel >= 2 ? '#00ff88' : '#5a7a9a'} fontSize="5.5">gamma</text>
        </g>

        {/* Engine pods — level 3 */}
        <g opacity={moduleOpacity(3)}>
          <line x1="150" y1="118" x2="150" y2="155" stroke="#1e3a5f" strokeWidth="1.5" />
          <rect x="130" y="155" width="40" height="16" rx="3" fill="#0d1224"
            stroke={currentLevel >= 3 ? '#a78bfa' : '#1e3a5f'} strokeWidth="1.5" />
          <text x="150" y="166" textAnchor="middle" fill={currentLevel >= 3 ? '#a78bfa' : '#5a7a9a'} fontSize="6">ENGINES</text>
        </g>

        {/* Sensor array — level 3 */}
        <g opacity={moduleOpacity(3)}>
          <line x1="132" y1="90" x2="100" y2="68" stroke="#1e3a5f" strokeWidth="1.5" />
          <rect x="78" y="56" width="40" height="16" rx="3" fill="#0d1224"
            stroke={currentLevel >= 3 ? '#ffd700' : '#1e3a5f'} strokeWidth="1.5" />
          <text x="98" y="67" textAnchor="middle" fill={currentLevel >= 3 ? '#ffd700' : '#5a7a9a'} fontSize="6">SENSORS</text>
        </g>

        {/* Level indicator ring */}
        <circle cx="150" cy="100" r="24" fill="none"
          stroke={currentLevel === 3 ? '#a78bfa' : currentLevel === 2 ? '#ffd700' : '#4fc3f7'}
          strokeWidth="0.5" strokeDasharray="3,3" />
      </svg>

      {/* Pod legend */}
      <div className="ship-legend">
        <span className="legend-ok">● RUNNING</span>
        <span className="legend-warn">● WARN</span>
        <span className="legend-err">● FAULT</span>
      </div>
    </div>
  )
}

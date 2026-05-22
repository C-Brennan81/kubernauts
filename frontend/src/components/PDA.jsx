const COMMANDS = [
  {
    game: 'scan <deployment>',
    kubectl: 'kubectl get pods -l app=<deployment>',
    desc: 'List all units in a deployment and their status.',
    tip: 'Start here when something seems wrong — get the overview first.',
  },
  {
    game: 'inspect <unit>',
    kubectl: 'kubectl describe pod <name>',
    desc: 'Show detailed info about a specific unit: status, events, restarts.',
    tip: 'Use this on PENDING or CRASH_LOOP units to find out why.',
  },
  {
    game: 'read logs <unit>',
    kubectl: 'kubectl logs <name>',
    desc: 'View the recent output/error logs from a unit.',
    tip: 'Logs tell you what the unit was doing before it died.',
  },
  {
    game: 'fix <unit>',
    kubectl: 'kubectl delete pod <name>',
    desc: 'Restart a unit by deleting it — the deployment recreates it.',
    tip: 'Works for CRASH_LOOP. The deployment controller brings it back fresh.',
  },
  {
    game: 'deploy reinforcements <deployment> --count=<n>',
    kubectl: 'kubectl scale deployment <name> --replicas=<n>',
    desc: 'Scale up a deployment to run more units.',
    tip: 'Use when a unit is OOM_KILLED — more replicas share the load.',
  },
  {
    game: 'revert mission <deployment>',
    kubectl: 'kubectl rollout undo deployment/<name>',
    desc: 'Roll back a deployment to its previous stable version.',
    tip: 'When a bad update crashes everything, rollback is your friend.',
  },
  {
    game: 'isolate <module>',
    kubectl: 'kubectl cordon <node>',
    desc: 'Mark a module as unschedulable — no new units assigned to it.',
    tip: 'Use when a node/module goes offline or is behaving badly.',
  },
  {
    game: 'status',
    kubectl: 'kubectl get nodes && kubectl get pods -A',
    desc: 'Full station overview — alert level, all units, all modules.',
    tip: 'Good first command when you log in or things look wrong.',
  },
]

export default function PDA({ onClose }) {
  return (
    <div className="pda-overlay" onClick={onClose}>
      <div className="pda-panel" onClick={e => e.stopPropagation()}>
        <div className="pda-header">
          <span className="pda-title">📟 PERSONAL DATA ASSISTANT</span>
          <span className="pda-sub">KLINK-approved command reference</span>
          <button className="pda-close" onClick={onClose}>✕</button>
        </div>
        <div className="pda-body">
          <div className="pda-note">
            "Everything in this game maps to a real kubectl command. You're learning Kubernetes.
            You just don't know it yet." — KLINK
          </div>
          {COMMANDS.map((c, i) => (
            <div key={i} className="pda-entry">
              <div className="pda-game-cmd">{c.game}</div>
              <div className="pda-kubectl">kubectl: <code>{c.kubectl}</code></div>
              <div className="pda-desc">{c.desc}</div>
              <div className="pda-tip">💡 {c.tip}</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

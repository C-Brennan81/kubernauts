import { useState, useRef, useEffect } from 'react'

const PROMPT = 'kubernaut@station:~$ '
const HISTORY_KEY = 'kubernauts_cmd_history'

const COMPLETIONS = [
  'scan crew-quarters', 'scan navigation', 'scan life-support',
  'inspect crew-alpha-1', 'inspect crew-alpha-2', 'inspect crew-beta-1',
  'inspect nav-system-1', 'inspect life-support-1',
  'read logs crew-alpha-1', 'read logs nav-system-1', 'read logs life-support-1',
  'fix crew-alpha-1', 'fix crew-alpha-2', 'fix crew-beta-1',
  'fix nav-system-1', 'fix life-support-1',
  'deploy reinforcements life-support --count=2',
  'deploy reinforcements crew-quarters --count=2',
  'revert mission crew-quarters', 'revert mission navigation',
  'isolate module-alpha', 'isolate module-beta', 'isolate module-gamma',
  'status', 'help', 'hint', 'clear',
  // new Phase 4 commands
  'exec crew-alpha-1 ls', 'exec nav-system-1 ls', 'exec life-support-1 ls',
  'label unit crew-alpha-1 env=prod', 'label unit nav-system-1 env=prod',
  'events crew-alpha-1', 'events nav-system-1', 'events life-support-1',
  'rollout status crew-quarters', 'rollout status navigation', 'rollout status life-support',
  // kubectl aliases
  'kubectl get pods', 'kubectl get nodes',
  'kubectl describe pod crew-alpha-1', 'kubectl describe pod nav-system-1',
  'kubectl describe pod life-support-1',
  'kubectl logs crew-alpha-1', 'kubectl logs nav-system-1',
  'kubectl delete pod crew-alpha-1',
  'kubectl scale deployment life-support --replicas=2',
  'kubectl rollout undo deployment/crew-quarters',
  'kubectl cordon module-beta',
]

export default function Terminal({ onCommand }) {
  const savedHistory = () => {
    try { return JSON.parse(localStorage.getItem(HISTORY_KEY)) || [] }
    catch { return [] }
  }

  const [input, setInput] = useState('')
  const [history, setHistory] = useState(savedHistory)
  const [histIdx, setHistIdx] = useState(-1)
  const [lines, setLines] = useState([{ type: 'system', text: 'Type "help" for commands. Tab to autocomplete. Real kubectl syntax works too.' }])
  const [suggestion, setSuggestion] = useState('')
  const bottomRef = useRef(null)
  const inputRef = useRef(null)

  useEffect(() => { bottomRef.current?.scrollIntoView({ behavior: 'smooth' }) }, [lines])
  useEffect(() => { inputRef.current?.focus() }, [])

  const pushLine = (type, text) => setLines(prev => [...prev, { type, text }])

  const submit = async () => {
    const cmd = input.trim()
    if (!cmd) return
    pushLine('input', PROMPT + cmd)
    const newHistory = [cmd, ...history.filter(h => h !== cmd)].slice(0, 100)
    setHistory(newHistory)
    localStorage.setItem(HISTORY_KEY, JSON.stringify(newHistory))
    setHistIdx(-1)
    setInput('')
    setSuggestion('')
    const output = await onCommand(cmd)
    if (output === '__CLEAR__') {
      setLines([{ type: 'system', text: 'Terminal cleared.' }])
    } else if (output) {
      pushLine('output', output)
    }
  }

  const updateSuggestion = (val) => {
    if (!val.trim()) { setSuggestion(''); return }
    const match = COMPLETIONS.find(c => c.startsWith(val) && c !== val)
    setSuggestion(match ? match.slice(val.length) : '')
  }

  const onKey = e => {
    if (e.key === 'Enter') { submit(); return }

    if (e.key === 'Tab') {
      e.preventDefault()
      if (suggestion) { setInput(input + suggestion); setSuggestion('') }
      return
    }

    if (e.key === 'ArrowUp') {
      e.preventDefault()
      const idx = Math.min(histIdx + 1, history.length - 1)
      setHistIdx(idx)
      const val = history[idx] ?? ''
      setInput(val)
      updateSuggestion(val)
      return
    }
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      const idx = Math.max(histIdx - 1, -1)
      setHistIdx(idx)
      const val = idx === -1 ? '' : history[idx]
      setInput(val)
      updateSuggestion(val)
      return
    }
  }

  const onChange = e => {
    setInput(e.target.value)
    updateSuggestion(e.target.value)
    setHistIdx(-1)
  }

  return (
    <div className="terminal" onClick={() => inputRef.current?.focus()}>
      <div className="terminal-header">
        <span className="terminal-title">STATION TERMINAL</span>
        <span className="terminal-hint">TAB to complete · ↑↓ history · real kubectl works</span>
      </div>
      <div className="terminal-body">
        {lines.map((l, i) => (
          <div key={i} className={`line line-${l.type}`}>
            <pre>{l.text}</pre>
          </div>
        ))}
        <div className="input-row">
          <span className="prompt">{PROMPT}</span>
          <div className="input-wrap">
            <input
              ref={inputRef}
              value={input}
              onChange={onChange}
              onKeyDown={onKey}
              autoComplete="off"
              spellCheck={false}
            />
            {suggestion && <span className="autocomplete-ghost">{suggestion}</span>}
          </div>
        </div>
        <div ref={bottomRef} />
      </div>
    </div>
  )
}

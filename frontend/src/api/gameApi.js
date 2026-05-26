const BASE = '/api/game'

export async function startGame(playerName) {
  const res = await fetch(`${BASE}/start`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ playerName })
  })
  return res.json()
}

export async function sendCommand(sessionId, command) {
  const res = await fetch(`${BASE}/command/${sessionId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ command })
  })
  return res.json()
}

export async function getState(sessionId) {
  const res = await fetch(`${BASE}/state/${sessionId}`)
  return res.json()
}

export async function getIdleComment() {
  const res = await fetch(`${BASE}/klink/idle`)
  const data = await res.json()
  return data.message
}

export async function getObjectives(sessionId) {
  const res = await fetch(`${BASE}/objectives/${sessionId}`)
  return res.json()
}

export async function getLeaderboard() {
  const res = await fetch(`${BASE}/leaderboard`)
  return res.json()
}

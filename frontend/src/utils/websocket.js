export class CollabSocket {
  constructor(docId, token) {
    this.docId = docId
    this.token = token
    this.ws = null
    this.callbacks = new Map()
    this.heartbeatTimer = null
    this.reconnectTimer = null
    this.reconnectCount = 0
    this.maxReconnect = 5
    this.closed = false
    this.heartbeatInterval = 30000
  }

  connect() {
    if (this.closed) return

    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = location.host
    const url = `${protocol}//${host}/ws/collaborate?docId=${this.docId}&token=${this.token}`

    this.ws = new WebSocket(url)

    this.ws.onopen = () => {
      this.reconnectCount = 0
      this._startHeartbeat()
      this._emit('connected')
    }

    this.ws.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data)
        if (msg.type === 'pong') return
        if (msg.type === 'sync' && msg.data?.heartbeatInterval) {
          this.heartbeatInterval = msg.data.heartbeatInterval * 1000
          this._stopHeartbeat()
          this._startHeartbeat()
        }
        this._emit('message', msg)
        if (msg.type) {
          this._emit(msg.type, msg.data || msg)
        }
      } catch (e) {
        console.warn('WebSocket message parse error:', e)
      }
    }

    this.ws.onclose = () => {
      this._stopHeartbeat()
      this._emit('disconnected')
      this._tryReconnect()
    }

    this.ws.onerror = () => {
      this._emit('error')
    }
  }

  sendOperation(ops, baseVersion) {
    this._send({
      type: 'operation',
      data: { ops, baseVersion }
    })
  }

  sendCursor(position) {
    this._send({
      type: 'cursor',
      data: { position }
    })
  }

  onMessage(event, callback) {
    if (!this.callbacks.has(event)) {
      this.callbacks.set(event, [])
    }
    this.callbacks.get(event).push(callback)
  }

  close() {
    this.closed = true
    this._stopHeartbeat()
    clearTimeout(this.reconnectTimer)
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
  }

  _send(data) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(data))
    }
  }

  _emit(event, data) {
    const cbs = this.callbacks.get(event) || []
    cbs.forEach(cb => cb(data))
  }

  _startHeartbeat() {
    this.heartbeatTimer = setInterval(() => {
      this._send({ type: 'ping' })
    }, this.heartbeatInterval)
  }

  _stopHeartbeat() {
    clearInterval(this.heartbeatTimer)
    this.heartbeatTimer = null
  }

  _tryReconnect() {
    if (this.closed || this.reconnectCount >= this.maxReconnect) return
    this.reconnectCount++
    const delay = Math.min(1000 * Math.pow(2, this.reconnectCount - 1), 30000)
    this.reconnectTimer = setTimeout(() => {
      this.connect()
    }, delay)
  }
}

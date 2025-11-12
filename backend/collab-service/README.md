# Collaboration Service

Real-time collaboration service for Papairs using Tiptap and Hocuspocus.

## Features

- Real-time collaborative editing using WebSockets
- Document synchronization using Y.js CRDT
- Multiple users can edit the same document simultaneously
- Automatic conflict resolution

## Endpoints

### HTTP API (Port 3005)
- `GET /health` - Health check endpoint

### WebSocket (Port 3004)
- `ws://localhost:3004` - WebSocket connection for collaboration

## Environment Variables

None required for basic functionality. Add these for production:

- `PORT` - WebSocket port (default: 3004)

## Usage

```javascript
// Client-side connection example
import { HocuspocusProvider } from '@hocuspocus/provider'

const provider = new HocuspocusProvider({
  url: 'ws://localhost:3004',
  name: 'document-name',
})
```

## Development

```bash
npm install
npm run dev
```

## Production

```bash
npm install --production
npm start
```

## Integration with Tiptap

This service works seamlessly with Tiptap's collaboration extension. See the frontend implementation in `DocsView.vue` for client-side integration.

## TODO

- [ ] Implement authentication
- [ ] Add database persistence
- [ ] Add user presence indicators
- [ ] Add document versioning
- [ ] Add access control per document

require('dotenv').config();

const express = require('express');
const http = require('http');
const path = require('path');
const { Server } = require('socket.io');
const { AccessToken } = require('livekit-server-sdk');

const app = express();
const server = http.createServer(app);
const io = new Server(server);

app.use(express.json({ limit: '10kb' }));
app.use(express.static(path.join(__dirname, 'public')));

app.post('/get-token', async (req, res) => {
  try {
    const room = String(req.body?.room || '').trim().slice(0, 60);
    const name = String(req.body?.name || 'مشارك').trim().slice(0, 30);

    if (!room) {
      return res.status(400).json({ error: 'اسم الغرفة مطلوب' });
    }

    if (
      !process.env.LIVEKIT_URL ||
      !process.env.LIVEKIT_API_KEY ||
      !process.env.LIVEKIT_API_SECRET
    ) {
      return res.status(500).json({ error: 'إعدادات LiveKit ناقصة' });
    }

    const token = new AccessToken(
      process.env.LIVEKIT_API_KEY,
      process.env.LIVEKIT_API_SECRET,
      { identity: `user-${require('crypto').randomUUID()}`, name }
    );

    token.addGrant({
      roomJoin: true,
      room,
      canPublish: true,
      canSubscribe: true
    });

    res.json({
      serverUrl: process.env.LIVEKIT_URL,
      token: await token.toJwt()
    });
  } catch (error) {
    console.error('LiveKit token error:', error.message);
    res.status(500).json({ error: 'تعذر تجهيز المكالمة' });
  }
});

io.on('connection', socket => {
  socket.on('join-room', input => {
    const roomId = String(
      typeof input === 'string' ? input : input?.roomId || ''
    ).trim().slice(0, 60);

    const name = String(
      typeof input === 'object' ? input?.name || 'مشارك' : 'مشارك'
    ).trim().slice(0, 30) || 'مشارك';

    if (!roomId) return;

    if (socket.data.roomId) {
      socket.emit('room-error', 'أنت موجود في غرفة بالفعل.');
      return;
    }

    const room = io.sockets.adapter.rooms.get(roomId);
    const ids = room ? [...room] : [];

    if (ids.length >= 8) {
      socket.emit('room-error', 'الغرفة ممتلئة. الحد الأقصى 8 مشاركين.');
      return;
    }

    socket.join(roomId);
    socket.data.roomId = roomId;
    socket.data.name = name;

    socket.emit('room-joined', {
      participants: ids.map(id => ({
        id,
        name: io.sockets.sockets.get(id)?.data.name || 'مشارك'
      }))
    });

    socket.to(roomId).emit('user-joined', { id: socket.id, name });
  });

  socket.on('signal', ({ to, data } = {}) => {
    const roomId = socket.data.roomId;
    const target = io.sockets.sockets.get(to);
    if (!roomId || !target || target.data.roomId !== roomId || !data) return;
    io.to(to).emit('signal', { from: socket.id, data });
  });

  function leaveRoom() {
    const roomId = socket.data.roomId;
    if (!roomId) return;
    socket.to(roomId).emit('user-left', socket.id);
    socket.leave(roomId);
    socket.data.roomId = null;
    socket.data.name = null;
  }

  socket.on('leave-room', leaveRoom);
  socket.on('disconnect', leaveRoom);
});

server.listen(3000, '0.0.0.0', () => {
  console.log('وصال يعمل على المنفذ 3000');
});

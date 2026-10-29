require('dotenv').config();

const express = require('express');
const { AccessToken } = require('livekit-server-sdk');

const app = express();
app.use(express.json());

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
      return res.status(500).json({ error: 'إعدادات LiveKit ناقصة في الخادم' });
    }

    const token = new AccessToken(
      process.env.LIVEKIT_API_KEY,
      process.env.LIVEKIT_API_SECRET,
      { identity: `${name}-${Date.now()}` }
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

app.listen(3001, '127.0.0.1', () => {
  console.log('خدمة رموز وصال تعمل على المنفذ 3001');
});

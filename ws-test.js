const SockJS = require('sockjs-client');
const Stomp = require('stompjs');

const userId = 1;
const wsBaseUrl = 'http://localhost:18084';

console.log('Connecting to WebSocket:', `${wsBaseUrl}/ws/progress`);

const socket = new SockJS(`${wsBaseUrl}/ws/progress`);
const stompClient = Stomp.over(socket);

// Enable debug logging
stompClient.debug = (str) => {
  console.log('[STOMP Debug]', str);
};

stompClient.connect({}, (frame) => {
  console.log('Connected:', frame);

  // Subscribe to progress messages
  stompClient.subscribe(`/topic/progress/${userId}`, (message) => {
    const data = JSON.parse(message.body);
    console.log('[PROGRESS]', data);
  });

  // Subscribe to complete messages
  stompClient.subscribe(`/topic/complete/${userId}`, (message) => {
    const data = JSON.parse(message.body);
    console.log('[COMPLETE]', data);
  });

  console.log('Subscribed to:');
  console.log(`  - /topic/progress/${userId}`);
  console.log(`  - /topic/complete/${userId}`);
  console.log('Waiting for messages... (will timeout in 30s)');

  // Timeout after 30 seconds
  setTimeout(() => {
    console.log('Timeout - disconnecting');
    stompClient.disconnect();
    process.exit(0);
  }, 30000);
}, (error) => {
  console.error('Connection error:', error);
  process.exit(1);
});
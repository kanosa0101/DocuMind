declare module 'stompjs' {
  namespace Stomp {
    interface Message {
      body: string
      headers: Record<string, string>
      command: string
    }

    interface Client {
      connect(headers: any, connectCallback: () => void, errorCallback: (error: any) => void): void
      disconnect(disconnectCallback: () => void): void
      subscribe(destination: string, callback: (message: Message) => void, headers?: any): any
      send(destination: string, headers?: any, body?: string): void
      connected: boolean
    }

    function over(urlOrWebSocket: string | WebSocket): Client
  }

  export = Stomp
}
import { useEffect, useState } from 'react';

const HomePage = () => {
  const [messages, setMessages] = useState<string[]>([]);
  const [isConnecting, setIsConnecting] = useState(false);
  const [eventSource, setEventSource] = useState<EventSource | null>(null);

  const startProcessing = () => {
    if (eventSource) {
      eventSource.close(); // Close previous connection if exists
    }
    setIsConnecting(true);
    setMessages([]); // Clear previous messages

    // Create a new EventSource connected to the Spring Boot SSE endpoint
    const newEventSource = new EventSource('http://localhost:8080/api/process'); // Spring Boot URL

    newEventSource.onopen = () => {
      console.log('Next.js: SSE connection to Spring Boot opened');
      setIsConnecting(false);
      setMessages(prev => [...prev, 'Connection opened!']);
    };

    newEventSource.onmessage = (event) => {
      console.log('Next.js received:', event.data);
      const newMessage = event.data;

      if (newMessage === 'FINISHED') {
         setMessages(prev => [...prev, 'Processing finished!']);
         newEventSource.close(); // Close on completion signal
         setEventSource(null);
      } else {
         setMessages(prev => [...prev, newMessage]);
      }
    };

    newEventSource.onerror = (error) => {
      console.error('Next.js: SSE error:', error);
      setIsConnecting(false);
      setMessages(prev => [...prev, 'Error occurred. Connection closed.']);
      newEventSource.close();
      setEventSource(null); // Clear event source on error
    };

    setEventSource(newEventSource);
  };

  // Cleanup function to close the connection when the component unmounts
  useEffect(() => {
    return () => {
      if (eventSource) {
        console.log('Next.js: Closing SSE connection.');
        eventSource.close();
      }
    };
  }, [eventSource]); // Depend on eventSource to ensure cleanup runs when it changes

  return (
    <div>
      <h1>SSE Example (Next.js &lt;- Spring Boot &lt;- FastAPI)</h1>
      <button onClick={startProcessing} disabled={isConnecting}>
        {isConnecting ? 'Connecting...' : 'Start AI Processing'}
      </button>
      <h2>Messages:</h2>
      <ul>
        {messages.map((msg, index) => (
          <li key={index}>{msg}</li>
        ))}
      </ul>
    </div>
  );
};

export default HomePage;
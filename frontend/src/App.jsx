import { useState, useRef, useEffect } from 'react';
import ReactMarkdown from 'react-markdown';
import './App.css';
import geminiImage from './assets/wappen.png';

function App() {
  const [messages, setMessages] = useState([]);
  const [inputVal, setInputVal] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [hasStarted, setHasStarted] = useState(false);
  const [displayedText, setDisplayedText] = useState('');
  
  const chatMessagesRef = useRef(null);
  const scrollToBottom = () => {
    if (chatMessagesRef.current) {
      chatMessagesRef.current.scrollTo({
        top: chatMessagesRef.current.scrollHeight,
        behavior: 'smooth'
      });
    }
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, isLoading]);

  useEffect(() => {
    const fullText = 'Textanfragen über den OpenData Bestand der Stadt Nürnberg';
    let currentIndex = 0;
    
    const typeInterval = setInterval(() => {
      if (currentIndex <= fullText.length) {
        setDisplayedText(fullText.substring(0, currentIndex));
        currentIndex++;
      } else {
        clearInterval(typeInterval);
      }
    }, 50);
    
    return () => clearInterval(typeInterval);
  }, []);

  const handleSend = async (overrideMsg) => {
    const textToSend = typeof overrideMsg === 'string' ? overrideMsg : inputVal;
    if (!textToSend.trim()) return;
    
    const userMsg = textToSend.trim();
    setMessages(prev => [...prev, { role: 'user', content: userMsg }]);
    setInputVal('');
    setIsLoading(true);
    setHasStarted(true);

    try {
      const isLocalHost = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
      const apiUrl = import.meta.env.VITE_API_URL || (isLocalHost ? 'http://127.0.0.1:8095' : 'https://api.nbg-pt.de');
      const response = await fetch(`${apiUrl}/api/chat/stream`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ question: userMsg })
      });

      if (!response.body) throw new Error("Kein ReadableStream verfügbar");

      setMessages(prev => [...prev, { role: 'assistant', content: '' }]);
      setIsLoading(false);

      const reader = response.body.getReader();
      const decoder = new TextDecoder('utf-8');
      let done = false;
      let streamedResponse = "";
      let buffer = "";

      while (!done) {
        const { value, done: doneReading } = await reader.read();
        done = doneReading;

        if (value) {
          buffer += decoder.decode(value, { stream: true });
          const lines = buffer.split('\n');
          buffer = lines.pop() || "";
          
          for (const line of lines) {
            if (line.startsWith('data:')) {
              let jsonStr = line.substring(5).trim();
              if (!jsonStr) continue;
              
              try {
                const obj = JSON.parse(jsonStr);
                if (obj.text) {
                  streamedResponse += obj.text;
                  setMessages(prev => {
                    const newMessages = [...prev];
                    newMessages[newMessages.length - 1] = { 
                      role: 'assistant', 
                      content: streamedResponse 
                    };
                    return newMessages;
                  });
                }
              } catch(e) {
              }
            }
          }
        }
      }
    } catch (error) {
      console.error("Error fetching chat:", error);
      setMessages(prev => [...prev, { role: 'assistant', content: "Fehler: Konnte keine Verbindung zum Server herstellen." }]);
      setIsLoading(false);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleReset = () => {
    setMessages([]);
    setInputVal('');
    setHasStarted(false);
  };

  return (
    <div className="app-container">
      <div className="main-wrapper">
        <div className="left-panel">
          {!hasStarted ? (
            <div className="center-content">
              <div className="hero-heading">NBG-PT</div>
              <div className="hero-subheading">{displayedText}</div>
              
              <div 
                className="example-query" 
                onClick={() => handleSend("Welche Datenkategorien sammelt die Stadt Nürnberg?")}
              >
                Welche Datenkategorien sammelt die Stadt Nürnberg?
              </div>

              <div className="input-container">
                <textarea 
                  className="chat-input"
                  placeholder="Frag mich etwas..."
                  value={inputVal}
                  onChange={(e) => setInputVal(e.target.value)}
                  onKeyDown={handleKeyDown}
                  disabled={isLoading}
                  rows={1}
                  spellCheck={false}
                />
                <button 
                  className="send-button" 
                  onClick={handleSend}
                  disabled={isLoading || !inputVal.trim()}
                  title="Senden"
                >
                  ➤
                </button>
              </div>
            </div>
          ) : (
            <div className="chat-layout">
              <div 
                className="chat-header clickable-header" 
                onClick={handleReset}
                title="Neuen Chat beginnen"
              >
                NBG-PT
              </div>
              
              <div className="chat-messages" ref={chatMessagesRef}>
                {messages.map((msg, index) => (
                  <div key={index} className={`message ${msg.role}`}>
                    {msg.role === 'assistant' ? (
                      <ReactMarkdown>{msg.content}</ReactMarkdown>
                    ) : (
                      msg.content
                    )}
                  </div>
                ))}
                
                {isLoading && (
                  <div className="message assistant loading">
                    <span className="loading-dots">
                      <span className="dot"></span>
                      <span className="dot"></span>
                      <span className="dot"></span>
                    </span>
                  </div>
                )}
              </div>

              <div className="chat-input-wrapper">
                <div className="input-container">
                  <textarea 
                    className="chat-input"
                    placeholder="Stelle eine weitere Frage..."
                    value={inputVal}
                    onChange={(e) => setInputVal(e.target.value)}
                    onKeyDown={handleKeyDown}
                    disabled={isLoading}
                    rows={1}
                    spellCheck={false}
                  />
                  <button 
                    className="send-button" 
                    onClick={handleSend}
                    disabled={isLoading || !inputVal.trim()}
                    title="Senden"
                  >
                    ➤
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
        
        <div className="right-panel">
          <img src={geminiImage} alt="Gemini Created" className="side-image" />
        </div>
      </div>

      <footer className="footer-bar">
        <div className="footer-content">
          <div className="footer-column">
            <h4 className="footer-heading">Quelle</h4>
            <a href="https://nuernberg.bydata.de/?locale=de" target="_blank" rel="noopener noreferrer">BayernData.Nuernberg</a>
            <a href="https://github.com/konstantinMllr/nbg-pt" target="_blank" rel="noopener noreferrer">GitHub Profil</a>
          </div>
          <div className="footer-column">
            <h4 className="footer-heading">Autor</h4>
            <a href="https://konstantin-m.me" target="_blank" rel="noopener noreferrer">konstantin-m.me</a>
          </div>
        </div>
      </footer>
    </div>
  );
}

export default App;

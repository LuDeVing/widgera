import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { promptApi } from '../services/api';

const History = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchHistory();
  }, []);

  const fetchHistory = async () => {
    try {
      const response = await promptApi.getAllHistory();
      setHistory(response.data);
    } catch (err) {
      setError('Failed to load history');
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleString();
  };

  return (
    <div className="container">
      <div className="header">
        <h1>History</h1>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <span style={{ color: '#666' }}>Welcome, {user?.username}</span>
          <Link to="/" className="nav-link">
            Dashboard
          </Link>
          <button className="btn btn-secondary" onClick={handleLogout}>
            Logout
          </button>
        </div>
      </div>

      {error && <div className="error">{error}</div>}

      {loading ? (
        <div className="card">
          <div className="loading">
            <div className="spinner"></div>
            <p>Loading history...</p>
          </div>
        </div>
      ) : history.length === 0 ? (
        <div className="card">
          <p style={{ textAlign: 'center', color: '#666' }}>
            No history yet. Go to the{' '}
            <Link to="/" className="nav-link">
              Dashboard
            </Link>{' '}
            to create your first prompt.
          </p>
        </div>
      ) : (
        history.map((item) => (
          <div className="card" key={item.id}>
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                marginBottom: '12px',
              }}
            >
              <small style={{ color: '#666' }}>
                {formatDate(item.createdAt)}
              </small>
              <small style={{ color: '#666' }}>ID: {item.id}</small>
            </div>

            <div style={{ marginBottom: '16px' }}>
              <strong>Prompt:</strong>
              <p
                style={{
                  backgroundColor: '#f5f5f5',
                  padding: '12px',
                  borderRadius: '4px',
                  marginTop: '8px',
                }}
              >
                {item.prompt}
              </p>
            </div>

            {item.imageUrl && (
              <div style={{ marginBottom: '16px' }}>
                <strong>Image:</strong>
                <div style={{ marginTop: '8px' }}>
                  <img
                    src={item.imageUrl}
                    alt="Prompt"
                    style={{
                      maxWidth: '150px',
                      maxHeight: '150px',
                      borderRadius: '4px',
                      border: '1px solid #ddd',
                    }}
                  />
                </div>
              </div>
            )}

            <div style={{ marginBottom: '16px' }}>
              <strong>Fields:</strong>
              <div
                style={{
                  display: 'flex',
                  gap: '8px',
                  marginTop: '8px',
                  flexWrap: 'wrap',
                }}
              >
                {item.fields.map((field, index) => (
                  <span
                    key={index}
                    style={{
                      backgroundColor:
                        field.type === 'number' ? '#e3f2fd' : '#e8f5e9',
                      padding: '4px 12px',
                      borderRadius: '16px',
                      fontSize: '14px',
                    }}
                  >
                    {field.name} ({field.type})
                  </span>
                ))}
              </div>
            </div>

            <div>
              <strong>Output:</strong>
              <div
                style={{
                  backgroundColor: '#f5f5f5',
                  padding: '12px',
                  borderRadius: '4px',
                  marginTop: '8px',
                  borderLeft: '4px solid #4CAF50',
                }}
              >
                {item.fields.map((field) => (
                  <div key={field.name} style={{ marginBottom: '4px' }}>
                    <strong>{field.name}:</strong>{' '}
                    <span
                      style={{
                        color: field.type === 'number' ? '#2196F3' : '#333',
                      }}
                    >
                      {item.output[field.name] !== undefined
                        ? String(item.output[field.name])
                        : 'N/A'}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        ))
      )}
    </div>
  );
};

export default History;

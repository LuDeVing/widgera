import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { promptApi } from '../services/api';
import FieldBuilder from '../components/FieldBuilder';
import ImageUpload from '../components/ImageUpload';
import OutputDisplay from '../components/OutputDisplay';

const Dashboard = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [prompt, setPrompt] = useState('');
  const [fields, setFields] = useState([{ name: '', type: 'string' }]);
  const [imageId, setImageId] = useState(null);
  const [imagePreviewUrl, setImagePreviewUrl] = useState(null);
  const [imageUploading, setImageUploading] = useState(false);
  const [output, setOutput] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const validateFields = () => {
    for (const field of fields) {
      if (!field.name.trim()) {
        return 'All field names are required';
      }
      // Check for valid field name (alphanumeric and underscore)
      if (!/^[a-zA-Z_][a-zA-Z0-9_]*$/.test(field.name)) {
        return 'Field names must start with a letter and contain only letters, numbers, and underscores';
      }
    }
    // Check for duplicate field names
    const names = fields.map((f) => f.name);
    if (new Set(names).size !== names.length) {
      return 'Field names must be unique';
    }
    return null;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setOutput(null);

    const validationError = validateFields();
    if (validationError) {
      setError(validationError);
      return;
    }

    setLoading(true);

    try {
      const response = await promptApi.submit({
        prompt,
        fields,
        imageId: imageId || null,
      });
      setOutput(response.data.output);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to process prompt');
    } finally {
      setLoading(false);
    }
  };

  const handleImageUpload = (id, previewUrl) => {
    setImageId(id);
    setImagePreviewUrl(previewUrl);
  };

  const clearForm = () => {
    setPrompt('');
    setFields([{ name: '', type: 'string' }]);
    setImageId(null);
    setImagePreviewUrl(null);
    setOutput(null);
    setError('');
  };

  return (
    <div className="container">
      <div className="header">
        <h1>Widgera</h1>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <span style={{ color: '#666' }}>Welcome, {user?.username}</span>
          <Link to="/history" className="nav-link">
            History
          </Link>
          <button className="btn btn-secondary" onClick={handleLogout}>
            Logout
          </button>
        </div>
      </div>

      <div className="card">
        <form onSubmit={handleSubmit}>
          {error && <div className="error">{error}</div>}

          <div style={{ marginBottom: '20px' }}>
            <label className="label">Prompt</label>
            <textarea
              className="textarea"
              value={prompt}
              onChange={(e) => setPrompt(e.target.value)}
              placeholder="Enter your prompt here..."
              required
            />
          </div>

          <ImageUpload onUpload={handleImageUpload} imageId={imageId} previewUrl={imagePreviewUrl} onUploadingChange={setImageUploading} />

          <div style={{ marginBottom: '20px' }}>
            <FieldBuilder fields={fields} setFields={setFields} />
          </div>

          <div style={{ display: 'flex', gap: '12px' }}>
            <button
              type="submit"
              className="btn btn-primary"
              style={{ flex: 1 }}
              disabled={loading || imageUploading}
            >
              {imageUploading ? 'Uploading image...' : loading ? 'Processing...' : 'Submit'}
            </button>
            <button
              type="button"
              className="btn"
              style={{ backgroundColor: '#9e9e9e', color: 'white' }}
              onClick={clearForm}
            >
              Clear
            </button>
          </div>
        </form>
      </div>

      {loading && (
        <div className="card">
          <div className="loading">
            <div className="spinner"></div>
            <p>Processing your request with Gemini AI...</p>
          </div>
        </div>
      )}

      {output && (
        <div className="card">
          <OutputDisplay output={output} fields={fields} />
        </div>
      )}
    </div>
  );
};

export default Dashboard;

import React, { useState } from 'react';
import { imageApi } from '../services/api';

const ImageUpload = ({ onUpload, imageId, previewUrl, onUploadingChange }) => {
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');
  const [filename, setFilename] = useState('');

  const handleFileChange = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      setError('Please select an image file');
      return;
    }

    // 10MB max
    if (file.size > 10 * 1024 * 1024) {
      setError('File size must be less than 10MB');
      return;
    }

    setError('');
    setUploading(true);
    onUploadingChange?.(true);
    setFilename(file.name);

    try {
      const response = await imageApi.upload(file);

      onUpload(response.data.imageId, response.data.imageUrl);
      if (response.data.duplicate) {
        setError('Image already uploaded previously');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to upload image');
      setFilename('');
    } finally {
      setUploading(false);
      onUploadingChange?.(false);
    }
  };

  const clearImage = () => {
    onUpload(null, null);
    setFilename('');
    setError('');
  };

  return (
    <div style={{ marginBottom: '16px' }}>
      <label className="label">Image (optional)</label>

      {error && <div className="error" style={{ marginBottom: '8px' }}>{error}</div>}

      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
        <label
          style={{
            display: 'inline-block',
            padding: '12px 24px',
            backgroundColor: '#f5f5f5',
            border: '1px solid #ddd',
            borderRadius: '4px',
            cursor: uploading ? 'not-allowed' : 'pointer',
          }}
        >
          {uploading ? 'Uploading...' : 'Choose File'}
          <input
            type="file"
            accept="image/*"
            onChange={handleFileChange}
            disabled={uploading}
            style={{ display: 'none' }}
          />
        </label>

        <span style={{ color: '#666' }}>
          {filename || 'No file chosen'}
        </span>

        {imageId && (
          <button
            type="button"
            className="btn btn-danger"
            onClick={clearImage}
            style={{ marginLeft: 'auto' }}
          >
            Remove
          </button>
        )}
      </div>

      {previewUrl && (
        <div style={{ marginTop: '12px' }}>
          <img
            src={previewUrl}
            alt="Uploaded"
            style={{
              maxWidth: '200px',
              maxHeight: '200px',
              borderRadius: '4px',
              border: '1px solid #ddd',
            }}
          />
        </div>
      )}
    </div>
  );
};

export default ImageUpload;

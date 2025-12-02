import React from 'react';

const OutputDisplay = ({ output, fields }) => {
  if (!output || Object.keys(output).length === 0) {
    return null;
  }

  return (
    <div
      style={{
        backgroundColor: '#f5f5f5',
        padding: '20px',
        borderRadius: '4px',
        borderLeft: '4px solid #4CAF50',
      }}
    >
      <h3 style={{ marginBottom: '16px' }}>Structured Output:</h3>

      {fields.map((field) => (
        <div key={field.name} style={{ marginBottom: '8px' }}>
          <strong>{field.name}:</strong>{' '}
          <span style={{ color: field.type === 'number' ? '#2196F3' : '#333' }}>
            {output[field.name] !== undefined ? String(output[field.name]) : 'N/A'}
          </span>
        </div>
      ))}
    </div>
  );
};

export default OutputDisplay;

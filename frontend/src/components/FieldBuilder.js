import React from 'react';

const FieldBuilder = ({ fields, setFields }) => {
  const addField = () => {
    setFields([...fields, { name: '', type: 'string' }]);
  };

  const removeField = (index) => {
    if (fields.length > 1) {
      setFields(fields.filter((_, i) => i !== index));
    }
  };

  const updateField = (index, key, value) => {
    const newFields = [...fields];
    newFields[index] = { ...newFields[index], [key]: value };
    setFields(newFields);
  };

  return (
    <div>
      <label className="label">Response Structure</label>

      {fields.map((field, index) => (
        <div
          key={index}
          style={{
            display: 'flex',
            gap: '12px',
            marginBottom: '12px',
            alignItems: 'center',
          }}
        >
          <input
            type="text"
            className="input"
            style={{ flex: 1, marginBottom: 0 }}
            placeholder="Field name"
            value={field.name}
            onChange={(e) => updateField(index, 'name', e.target.value)}
            required
          />

          <select
            className="select"
            value={field.type}
            onChange={(e) => updateField(index, 'type', e.target.value)}
          >
            <option value="string">String</option>
            <option value="number">Number</option>
          </select>

          {fields.length > 1 && (
            <button
              type="button"
              className="btn btn-danger"
              onClick={() => removeField(index)}
              title="Remove field"
            >
              ×
            </button>
          )}
        </div>
      ))}

      <button
        type="button"
        className="btn btn-secondary"
        onClick={addField}
        style={{ marginTop: '8px' }}
      >
        + Add Field
      </button>
    </div>
  );
};

export default FieldBuilder;

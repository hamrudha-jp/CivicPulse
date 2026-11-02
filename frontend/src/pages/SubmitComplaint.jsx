import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { Camera, Send, AlertCircle, CheckCircle, Navigation, Info } from 'lucide-react';

export default function SubmitComplaint({ currentUser, onSubmissionSuccess }) {
  const [departments, setDepartments] = useState([]);
  const [categories, setCategories] = useState([]);
  const [selectedDeptId, setSelectedDeptId] = useState('');
  const [selectedCatId, setSelectedCatId] = useState('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [severity, setSeverity] = useState('HIGH');
  const [latitude, setLatitude] = useState('12.9716');
  const [longitude, setLongitude] = useState('77.5946');
  const [addressLandmark, setAddressLandmark] = useState('MG Road near Metro Station, Bengaluru');
  const [photoFile, setPhotoFile] = useState(null);
  const [photoPreview, setPhotoPreview] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [submittedData, setSubmittedData] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadDepartments();
  }, []);

  const loadDepartments = async () => {
    try {
      const data = await api.departments.getAll();
      setDepartments(data);
      if (data.length > 0) {
        setSelectedDeptId(data[0].id.toString());
        loadCategories(data[0].id);
      }
    } catch (err) {
      setError('Failed to load departments: ' + err.message);
    }
  };

  const handleDeptChange = (deptId) => {
    setSelectedDeptId(deptId);
    loadCategories(deptId);
  };

  const loadCategories = async (deptId) => {
    try {
      const cats = await api.departments.getCategories(deptId);
      setCategories(cats);
      if (cats.length > 0) {
        setSelectedCatId(cats[0].id.toString());
        if (cats[0].defaultSeverity) {
          setSeverity(cats[0].defaultSeverity);
        }
      } else {
        setSelectedCatId('');
      }
    } catch (err) {
      console.error(err);
    }
  };

  const handleCategoryChange = (catId) => {
    setSelectedCatId(catId);
    const cat = categories.find(c => c.id.toString() === catId);
    if (cat?.defaultSeverity) {
      setSeverity(cat.defaultSeverity);
    }
  };

  const handlePhotoSelect = (e) => {
    const file = e.target.files[0];
    if (file) {
      setPhotoFile(file);
      setPhotoPreview(URL.createObjectURL(file));
    }
  };

  const setPresetLocation = (lat, lon, landmark) => {
    setLatitude(lat.toString());
    setLongitude(lon.toString());
    setAddressLandmark(landmark);
  };

  const handleGetLocation = () => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          setLatitude(pos.coords.latitude.toFixed(6));
          setLongitude(pos.coords.longitude.toFixed(6));
          setAddressLandmark('Current GPS Location');
        },
        (err) => alert('Could not get GPS location: ' + err.message)
      );
    } else {
      alert('Geolocation not supported by this browser.');
    }
  };

  const severityPoints = { CRITICAL: 10, HIGH: 7, MEDIUM: 4, LOW: 2 };
  const baseS = severityPoints[severity] || 4;
  const initialEstimatedScore = ((baseS * 0.45) + (1.0 * 0.30)).toFixed(2);

  const selectedCategoryObj = categories.find(c => c.id.toString() === selectedCatId);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!currentUser || currentUser.role !== 'ROLE_CITIZEN') {
      alert('You must be signed in as a Citizen to submit a complaint. Please use the top-right role switcher.');
      return;
    }

    if (!selectedCatId || !title.trim() || !description.trim() || !latitude || !longitude || !addressLandmark.trim()) {
      alert('Please fill out all required fields.');
      return;
    }

    try {
      setSubmitting(true);
      setError(null);

      const payload = {
        categoryId: parseInt(selectedCatId, 10),
        title: title.trim(),
        description: description.trim(),
        severity,
        latitude: parseFloat(latitude),
        longitude: parseFloat(longitude),
        addressLandmark: addressLandmark.trim()
      };

      let result;
      if (photoFile) {
        result = await api.complaints.submitWithPhoto(payload, photoFile);
      } else {
        result = await api.complaints.submitJson(payload);
      }

      setSubmittedData(result);
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  if (submittedData) {
    return (
      <div style={{ maxWidth: '640px', margin: '3rem auto' }}>
        <div className="glass-panel" style={{ padding: '3rem', textAlign: 'center' }}>
          <div style={{
            width: '60px',
            height: '60px',
            borderRadius: '50%',
            background: '#dcfce7',
            border: '1px solid #86efac',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto 1.5rem'
          }}>
            <CheckCircle size={32} color="#16a34a" />
          </div>

          <h2 style={{ fontSize: '1.75rem', fontWeight: 800, marginBottom: '0.5rem', color: 'var(--text-main)' }}>
            Complaint Registered Successfully!
          </h2>
          <p style={{ color: 'var(--text-muted)', marginBottom: '1.5rem', fontSize: '0.95rem' }}>
            Your issue has been logged into the municipal queue and evaluated by the priority engine.
          </p>

          <div style={{
            background: '#f8fafc',
            border: '1px solid var(--border-medium)',
            borderRadius: '12px',
            padding: '1.5rem',
            marginBottom: '2rem',
            textAlign: 'left'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
              <span style={{ color: 'var(--text-subtle)', fontSize: '0.85rem' }}>Tracking Number:</span>
              <span style={{ fontFamily: 'var(--font-mono)', fontSize: '1.1rem', fontWeight: 700, color: 'var(--primary)' }}>
                {submittedData.trackingNumber}
              </span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
              <span style={{ color: 'var(--text-subtle)', fontSize: '0.85rem' }}>Initial Priority Score:</span>
              <span style={{ fontWeight: 700, color: '#a16207' }}>
                {submittedData.priorityScore?.toFixed(2)} / 10.0
              </span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
              <span style={{ color: 'var(--text-subtle)', fontSize: '0.85rem' }}>Department:</span>
              <span style={{ fontWeight: 600 }}>{submittedData.departmentName}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: 'var(--text-subtle)', fontSize: '0.85rem' }}>Status:</span>
              <span className={`status-pill status-${submittedData.status?.toLowerCase().replace(/_/g, '-')}`}>
                {submittedData.status}
              </span>
            </div>
          </div>

          <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center' }}>
            <button
              onClick={() => onSubmissionSuccess(submittedData.trackingNumber)}
              className="btn btn-primary"
            >
              Track Live Timeline
            </button>
            <button
              onClick={() => {
                setSubmittedData(null);
                setTitle('');
                setDescription('');
                setPhotoFile(null);
                setPhotoPreview(null);
              }}
              className="btn btn-secondary"
            >
              Submit Another Issue
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: '820px', margin: '2rem auto' }}>

      {/* Title — with subtle page-level city accent */}
      <div className="page-city-accent" style={{ marginBottom: '2rem' }}>
        <h1 style={{ fontSize: '2rem', fontWeight: 800, letterSpacing: '-0.02em', marginBottom: '0.4rem', color: 'var(--text-main)' }}>
          File a Public Grievance
        </h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.95rem' }}>
          Citizen submissions are scored by the transparent prioritization engine and scanned for spatial duplicates.
        </p>
      </div>

      {error && (
        <div style={{
          background: '#fef2f2',
          border: '1px solid #fecaca',
          borderRadius: '10px',
          padding: '1rem',
          color: '#b91c1c',
          display: 'flex',
          alignItems: 'center',
          gap: '0.6rem',
          marginBottom: '1.5rem',
          fontSize: '0.9rem'
        }}>
          <AlertCircle size={18} /> {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="glass-panel" style={{ padding: '2.5rem' }}>

        {/* Department & Category Row */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.25rem', marginBottom: '1.25rem' }}>
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label className="form-label">Municipal Department *</label>
            <select
              className="form-select"
              value={selectedDeptId}
              onChange={(e) => handleDeptChange(e.target.value)}
              required
            >
              {departments.map(d => (
                <option key={d.id} value={d.id}>{d.name}</option>
              ))}
            </select>
          </div>

          <div className="form-group" style={{ marginBottom: 0 }}>
            <label className="form-label">Issue Category *</label>
            <select
              className="form-select"
              value={selectedCatId}
              onChange={(e) => handleCategoryChange(e.target.value)}
              required
            >
              {categories.map(c => (
                <option key={c.id} value={c.id}>
                  {c.name} ({c.slaHours}h SLA)
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* SLA Notice banner */}
        {selectedCategoryObj && (
          <div style={{
            background: 'var(--primary-light)',
            border: '1px solid #bfdbfe',
            borderRadius: '8px',
            padding: '0.75rem 1rem',
            fontSize: '0.82rem',
            color: 'var(--primary)',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
            marginBottom: '1.5rem'
          }}>
            <Info size={16} />
            <span>
              Target Resolution SLA: <strong>{selectedCategoryObj.slaHours} hours</strong>. If unresolved by deadline, system will escalate automatically.
            </span>
          </div>
        )}

        {/* Complaint Title */}
        <div className="form-group">
          <label className="form-label">Title / Short Summary *</label>
          <input
            type="text"
            className="form-input"
            placeholder="e.g. Broken water pipe leaking continuously near 3rd cross"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            minLength={5}
            maxLength={200}
            required
          />
        </div>

        {/* Detailed Description */}
        <div className="form-group">
          <label className="form-label">Detailed Description *</label>
          <textarea
            className="form-textarea"
            rows={4}
            placeholder="Provide specific details about the issue, affected residents, visible hazards..."
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            minLength={10}
            required
          />
        </div>

        {/* Severity Selector */}
        <div className="form-group">
          <label className="form-label">Estimated Severity *</label>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '0.75rem' }}>
            {[
              { val: 'CRITICAL', label: 'Critical (10 pts)', color: '#b91c1c', bgLight: '#fef2f2', desc: 'Direct life hazard or collapse' },
              { val: 'HIGH', label: 'High (7 pts)', color: '#c2410c', bgLight: '#fff7ed', desc: 'Severe civic disruption' },
              { val: 'MEDIUM', label: 'Medium (4 pts)', color: '#a16207', bgLight: '#fefce8', desc: 'Standard defect' },
              { val: 'LOW', label: 'Low (2 pts)', color: '#15803d', bgLight: '#f0fdf4', desc: 'Minor cosmetic flaw' },
            ].map(s => (
              <div
                key={s.val}
                onClick={() => setSeverity(s.val)}
                style={{
                  border: `1px solid ${severity === s.val ? s.color : 'var(--border-subtle)'}`,
                  background: severity === s.val ? s.bgLight : '#ffffff',
                  borderRadius: '10px',
                  padding: '0.75rem',
                  cursor: 'pointer',
                  transition: 'all 0.15s ease',
                  boxShadow: severity === s.val ? '0 1px 3px rgba(0,0,0,0.06)' : undefined
                }}
              >
                <div style={{ fontWeight: 700, fontSize: '0.85rem', color: s.color, marginBottom: '0.2rem' }}>
                  {s.label}
                </div>
                <div style={{ fontSize: '0.72rem', color: 'var(--text-subtle)' }}>
                  {s.desc}
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Location & GPS */}
        <div className="form-group">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.45rem' }}>
            <label className="form-label" style={{ marginBottom: 0 }}>Location & GPS Coordinates *</label>
            <div style={{ display: 'flex', gap: '0.4rem' }}>
              <button
                type="button"
                onClick={handleGetLocation}
                className="btn btn-sm btn-secondary"
                style={{ fontSize: '0.75rem', padding: '0.2rem 0.5rem' }}
              >
                <Navigation size={12} /> Detect GPS
              </button>
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', marginBottom: '0.75rem' }}>
            <input
              type="number"
              step="0.0001"
              className="form-input"
              placeholder="Latitude (e.g. 12.9716)"
              value={latitude}
              onChange={(e) => setLatitude(e.target.value)}
              required
            />
            <input
              type="number"
              step="0.0001"
              className="form-input"
              placeholder="Longitude (e.g. 77.5946)"
              value={longitude}
              onChange={(e) => setLongitude(e.target.value)}
              required
            />
          </div>

          <input
            type="text"
            className="form-input"
            placeholder="Street Address or Landmark (e.g. Near Metro Pillar 142, MG Road)"
            value={addressLandmark}
            onChange={(e) => setAddressLandmark(e.target.value)}
            required
            style={{ marginBottom: '0.6rem' }}
          />

          {/* Preset Location Test Shortcuts */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', flexWrap: 'wrap' }}>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-subtle)' }}>Quick Presets:</span>
            <button
              type="button"
              className="btn btn-sm btn-secondary"
              style={{ fontSize: '0.72rem', padding: '0.2rem 0.55rem' }}
              onClick={() => setPresetLocation(12.9716, 77.5946, 'MG Road Metro Station')}
            >
              MG Road
            </button>
            <button
              type="button"
              className="btn btn-sm btn-secondary"
              style={{ fontSize: '0.72rem', padding: '0.2rem 0.55rem' }}
              onClick={() => setPresetLocation(12.9780, 77.6400, 'Indiranagar 4th Cross')}
            >
              Indiranagar
            </button>
            <button
              type="button"
              className="btn btn-sm btn-secondary"
              style={{ fontSize: '0.72rem', padding: '0.2rem 0.55rem' }}
              onClick={() => setPresetLocation(12.9352, 77.6245, 'Koramangala 5th Block')}
            >
              Koramangala
            </button>
          </div>
        </div>

        {/* Photo Attachment (Optional) */}
        <div className="form-group">
          <label className="form-label">Attach Photo Evidence (Optional)</label>
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <label className="btn btn-secondary" style={{ cursor: 'pointer' }}>
              <Camera size={16} /> Choose Image
              <input
                type="file"
                accept="image/png, image/jpeg, image/jpg, image/webp"
                onChange={handlePhotoSelect}
                style={{ display: 'none' }}
              />
            </label>
            {photoPreview && (
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                <img
                  src={photoPreview}
                  alt="Preview"
                  style={{ width: '48px', height: '48px', objectFit: 'cover', borderRadius: '8px', border: '1px solid var(--border-medium)' }}
                />
                <button
                  type="button"
                  onClick={() => { setPhotoFile(null); setPhotoPreview(null); }}
                  style={{ background: 'none', border: 'none', color: '#b91c1c', fontSize: '0.78rem', cursor: 'pointer', fontWeight: 600 }}
                >
                  Remove
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Live Algorithmic Score Preview */}
        <div style={{
          background: '#f8fafc',
          border: '1px solid var(--border-medium)',
          borderRadius: '12px',
          padding: '1.25rem',
          marginBottom: '2rem',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between'
        }}>
          <div>
            <div style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-main)' }}>
              Initial Priority Engine Preview
            </div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-subtle)' }}>
              Base ({baseS} × 0.45) + Initial Community Signal (1.0 × 0.30)
            </div>
          </div>
          <div className="priority-chip score-high" style={{ fontSize: '1rem', padding: '0.4rem 0.8rem' }}>
            Est. Score: {initialEstimatedScore} / 10
          </div>
        </div>

        {/* Submit Button */}
        <button
          type="submit"
          disabled={submitting}
          className="btn btn-primary"
          style={{ width: '100%', padding: '0.85rem', fontSize: '1rem' }}
        >
          {submitting ? 'Registering with Priority Engine...' : (
            <>
              <Send size={18} /> Submit Complaint
            </>
          )}
        </button>
      </form>
    </div>
  );
}

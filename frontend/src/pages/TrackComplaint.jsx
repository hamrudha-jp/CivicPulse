import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { Search, CheckCircle2, Clock, AlertTriangle, MapPin, User, Building, Link as LinkIcon, ThumbsUp } from 'lucide-react';

export default function TrackComplaint({ initialTrackingNumber, currentUser }) {
  const [trackingNumber, setTrackingNumber] = useState(initialTrackingNumber || 'CP-2026-R0001');
  const [complaint, setComplaint] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (initialTrackingNumber) {
      setTrackingNumber(initialTrackingNumber);
      handleSearch(initialTrackingNumber);
    } else {
      handleSearch('CP-2026-R0001');
    }
  }, [initialTrackingNumber]);

  const handleSearch = async (numToSearch) => {
    const query = (numToSearch || trackingNumber).trim();
    if (!query) return;

    try {
      setLoading(true);
      setError(null);
      const data = await api.complaints.track(query);
      setComplaint(data);
    } catch (err) {
      setError(err.message);
      setComplaint(null);
    } finally {
      setLoading(false);
    }
  };

  const handleUpvote = async () => {
    if (!complaint) return;
    if (!currentUser || currentUser.role !== 'ROLE_CITIZEN') {
      alert('Only registered citizens can upvote civic complaints. Please switch to a Citizen profile in the top-right menu.');
      return;
    }
    try {
      const updated = await api.complaints.upvote(complaint.id);
      setComplaint(prev => ({
        ...prev,
        upvotesCount: updated.upvotesCount,
        priorityScore: updated.priorityScore
      }));
    } catch (err) {
      alert(err.message);
    }
  };

  const handleCancel = async () => {
    if (!complaint) return;
    if (!window.confirm("Are you sure you want to cancel this complaint? This action cannot be undone.")) {
      return;
    }
    try {
      const updated = await api.complaints.cancel(complaint.id);
      setComplaint(updated);
      alert("Complaint cancelled successfully.");
    } catch (err) {
      alert(err.message);
    }
  };

  const sampleTrackings = ['CP-2026-R0001', 'CP-2026-W0001', 'CP-2026-S0001'];

  // Status progression steps
  const steps = [
    { key: 'SUBMITTED', label: 'Submitted' },
    { key: 'ASSIGNED', label: 'Assigned' },
    { key: 'IN_PROGRESS', label: 'In Progress' },
    { key: 'RESOLVED', label: 'Resolved' }
  ];

  const getStepIndex = (status) => {
    if (status === 'RESOLVED') return 3;
    if (status === 'IN_PROGRESS') return 2;
    if (status === 'ASSIGNED') return 1;
    return 0;
  };

  const currentStepIdx = complaint ? getStepIndex(complaint.status) : 0;

  return (
    <div style={{ maxWidth: '860px', margin: '2rem auto' }}>

      {/* Search Bar — subtle city accent decorates the panel corner */}
      <div className="glass-panel page-city-accent" style={{ padding: '2rem', marginBottom: '2rem' }}>
        <h1 style={{ fontSize: '1.75rem', fontWeight: 800, marginBottom: '0.4rem', color: 'var(--text-main)' }}>
          Real-Time Grievance Tracker
        </h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.92rem', marginBottom: '1.25rem' }}>
          Enter a municipal tracking number to view real-time SLA progress, priority scores, and official resolution timeline.
        </p>

        <form onSubmit={(e) => { e.preventDefault(); handleSearch(); }} style={{ display: 'flex', gap: '0.75rem' }}>
          <div style={{ position: 'relative', flex: 1 }}>
            <input
              type="text"
              className="form-input"
              style={{ paddingLeft: '2.5rem', fontFamily: 'var(--font-mono)', fontSize: '1rem', textTransform: 'uppercase' }}
              placeholder="e.g. CP-2026-R0001"
              value={trackingNumber}
              onChange={(e) => setTrackingNumber(e.target.value)}
            />
            <Search size={18} color="#64748b" style={{ position: 'absolute', left: '1rem', top: '50%', transform: 'translateY(-50%)' }} />
          </div>
          <button type="submit" disabled={loading} className="btn btn-primary" style={{ padding: '0 1.5rem' }}>
            {loading ? 'Searching...' : 'Track'}
          </button>
        </form>

        {/* Quick Suggestion Chips */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '1rem', flexWrap: 'wrap' }}>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-subtle)' }}>Try Sample Issues:</span>
          {sampleTrackings.map(s => (
            <button
              key={s}
              type="button"
              className="btn btn-sm btn-secondary"
              style={{ fontSize: '0.75rem', padding: '0.2rem 0.6rem', fontFamily: 'var(--font-mono)' }}
              onClick={() => { setTrackingNumber(s); handleSearch(s); }}
            >
              {s}
            </button>
          ))}
        </div>
      </div>

      {error && (
        <div className="card-elevated" style={{ borderColor: '#fecaca', background: '#fef2f2', color: '#b91c1c', marginBottom: '2rem' }}>
          {error}
        </div>
      )}

      {complaint && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>

          {/* Main Complaint Header Card */}
          <div className="glass-panel" style={{ padding: '2rem' }}>

            {/* Duplicate Notice Banner if merged */}
            {complaint.status === 'DUPLICATE' && (
              <div style={{
                background: '#f1f5f9',
                border: '1px solid #cbd5e1',
                borderRadius: '8px',
                padding: '0.85rem 1.25rem',
                marginBottom: '1.5rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.75rem'
              }}>
                <LinkIcon size={18} color="#64748b" />
                <div style={{ fontSize: '0.88rem', color: 'var(--text-main)' }}>
                  <strong>Merged Duplicate Issue:</strong> This report has been verified as a spatial duplicate and linked to primary issue{' '}
                  <span
                    onClick={() => { setTrackingNumber(complaint.parentComplaintTrackingNumber); handleSearch(complaint.parentComplaintTrackingNumber); }}
                    style={{ color: 'var(--primary)', textDecoration: 'underline', cursor: 'pointer', fontFamily: 'var(--font-mono)', fontWeight: 700 }}
                  >
                    #{complaint.parentComplaintTrackingNumber}
                  </span>.
                  Your report has amplified the primary issue's community priority!
                </div>
              </div>
            )}

            {/* Cancelled Notice Banner if withdrawn */}
            {complaint.status === 'CANCELLED' && (
              <div style={{
                background: '#fef2f2',
                border: '1px solid #fecaca',
                borderRadius: '8px',
                padding: '0.85rem 1.25rem',
                marginBottom: '1.5rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.75rem',
                color: '#b91c1c'
              }}>
                <AlertTriangle size={18} color="#b91c1c" />
                <div style={{ fontSize: '0.88rem' }}>
                  <strong>Complaint Cancelled:</strong> This complaint was cancelled by the citizen while in SUBMITTED status and will not be processed further.
                </div>
              </div>
            )}

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem', marginBottom: '1rem' }}>
              <div>
                <span style={{ fontFamily: 'var(--font-mono)', fontSize: '0.95rem', color: 'var(--primary)', fontWeight: 700 }}>
                  #{complaint.trackingNumber}
                </span>
                <h2 style={{ fontSize: '1.45rem', fontWeight: 800, color: 'var(--text-main)', marginTop: '0.25rem' }}>
                  {complaint.title}
                </h2>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                <span className={`status-pill status-${complaint.status?.toLowerCase().replace(/_/g, '-')}`}>
                  {complaint.status?.replace('_', ' ')}
                </span>
                <span className={`badge badge-${complaint.severity?.toLowerCase()}`}>
                  {complaint.severity} Severity
                </span>
                <div className="priority-chip score-urgent" style={{ fontSize: '0.9rem', padding: '0.35rem 0.75rem' }}>
                  Priority: {complaint.priorityScore?.toFixed(2)}
                </div>
                {(!currentUser || currentUser?.role === 'ROLE_CITIZEN') && (
                  <button onClick={handleUpvote} className="btn btn-sm btn-secondary" style={{ borderRadius: '999px' }} title="Upvote issue">
                    <ThumbsUp size={13} /> {complaint.upvotesCount}
                  </button>
                )}
                {complaint.status === 'SUBMITTED' && currentUser && currentUser.role === 'ROLE_CITIZEN' && (currentUser.id === complaint.citizenId || currentUser.userId === complaint.citizenId) && (
                  <button
                    onClick={handleCancel}
                    className="btn btn-sm"
                    style={{ background: '#fee2e2', color: '#b91c1c', border: '1px solid #fca5a5' }}
                    title="Cancel this complaint"
                  >
                    Cancel Complaint
                  </button>
                )}
              </div>
            </div>

            <p style={{ color: 'var(--text-muted)', fontSize: '0.95rem', lineHeight: 1.6, marginBottom: '1.5rem' }}>
              {complaint.description}
            </p>

            {/* Stepper Timeline */}
            <div style={{ margin: '2rem 0', background: '#f8fafc', padding: '1.5rem', borderRadius: '12px', border: '1px solid var(--border-subtle)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', position: 'relative' }}>

                {/* Connecting Line */}
                <div style={{
                  position: 'absolute',
                  top: '16px',
                  left: '30px',
                  right: '30px',
                  height: '2px',
                  background: '#cbd5e1',
                  zIndex: 0
                }}></div>

                {steps.map((st, idx) => {
                  const isDone = idx <= currentStepIdx;
                  const isCurrent = idx === currentStepIdx;
                  return (
                    <div key={st.key} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', zIndex: 1, position: 'relative' }}>
                      <div style={{
                        width: '34px',
                        height: '34px',
                        borderRadius: '50%',
                        background: isDone ? 'var(--primary)' : '#ffffff',
                        border: `2px solid ${isCurrent ? 'var(--primary)' : isDone ? 'var(--primary)' : '#cbd5e1'}`,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: isDone ? '#ffffff' : '#64748b',
                        boxShadow: isCurrent ? '0 0 0 4px var(--primary-light)' : undefined
                      }}>
                        {isDone ? <CheckCircle2 size={16} /> : <Clock size={16} />}
                      </div>
                      <span style={{ fontSize: '0.78rem', fontWeight: isDone ? 700 : 500, color: isDone ? 'var(--text-main)' : 'var(--text-subtle)', marginTop: '0.5rem' }}>
                        {st.label}
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Meta Grid */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem', background: '#f8fafc', border: '1px solid var(--border-subtle)', padding: '1.25rem', borderRadius: '10px' }}>
              <div>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-subtle)', display: 'block' }}>Department</span>
                <span style={{ fontWeight: 600, fontSize: '0.9rem', color: 'var(--text-main)', display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                  <Building size={14} color="#64748b" /> {complaint.departmentName}
                </span>
              </div>

              <div>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-subtle)', display: 'block' }}>Category</span>
                <span style={{ fontWeight: 600, fontSize: '0.9rem' }}>{complaint.categoryName}</span>
              </div>

              <div>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-subtle)', display: 'block' }}>Assigned Official</span>
                <span style={{ fontWeight: 600, fontSize: '0.9rem', color: complaint.assignedOfficialName ? 'var(--text-main)' : '#a16207', display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                  <User size={14} color="#64748b" /> {complaint.assignedOfficialName || 'Unassigned (In Queue)'}
                </span>
              </div>

              <div>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-subtle)', display: 'block' }}>Location Landmark</span>
                <span style={{ fontWeight: 600, fontSize: '0.9rem', display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                  <MapPin size={14} color="#64748b" /> {complaint.addressLandmark}
                </span>
              </div>
            </div>

            {/* Resolution Remarks if resolved */}
            {complaint.status === 'RESOLVED' && (
              <div style={{ marginTop: '1.5rem', background: '#f0fdf4', border: '1px solid #bbf7d0', padding: '1.25rem', borderRadius: '10px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: '#15803d', fontWeight: 700, fontSize: '0.9rem', marginBottom: '0.35rem' }}>
                  <CheckCircle2 size={16} /> Official Resolution Statement
                </div>
                <p style={{ color: 'var(--text-main)', fontSize: '0.9rem', lineHeight: 1.5 }}>
                  {complaint.resolutionRemarks}
                </p>
                {complaint.resolvedAt && (
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-subtle)', marginTop: '0.5rem', display: 'block' }}>
                    Resolved on {new Date(complaint.resolvedAt).toLocaleString()}
                  </span>
                )}
              </div>
            )}
          </div>

          {/* Audit History Timeline Log */}
          <div className="glass-panel" style={{ padding: '2rem' }}>
            <h3 style={{ fontSize: '1.15rem', fontWeight: 700, marginBottom: '1.25rem', display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--text-main)' }}>
              <Clock size={18} color="var(--primary)" /> Audit Trail & Status Timeline
            </h3>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {complaint.statusHistory?.map((h, i) => (
                <div
                  key={h.id || i}
                  style={{
                    display: 'flex',
                    alignItems: 'flex-start',
                    gap: '1rem',
                    borderLeft: '2px solid #cbd5e1',
                    paddingLeft: '1.25rem',
                    position: 'relative'
                  }}
                >
                  <div style={{
                    position: 'absolute',
                    left: '-7px',
                    top: '4px',
                    width: '12px',
                    height: '12px',
                    borderRadius: '50%',
                    background: 'var(--primary)'
                  }}></div>

                  <div style={{ flex: 1 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.2rem' }}>
                      <span className={`status-pill status-${h.toStatus?.toLowerCase().replace(/_/g, '-')}`}>
                        {h.toStatus}
                      </span>
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-subtle)' }}>
                        {new Date(h.createdAt).toLocaleString()}
                      </span>
                    </div>
                    <div style={{ fontSize: '0.88rem', color: 'var(--text-main)', marginTop: '0.25rem' }}>
                      {h.remarks}
                    </div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-subtle)', marginTop: '0.2rem' }}>
                      Action by: {h.changedByUserName || 'System'}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

        </div>
      )}

    </div>
  );
}

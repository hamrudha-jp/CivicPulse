import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { ThumbsUp, MapPin, ArrowRight, ShieldAlert, Sparkles, Filter } from 'lucide-react';

export default function PublicFeed({ onTrackComplaint, currentUser }) {
  const [complaints, setComplaints] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedDept, setSelectedDept] = useState('ALL');
  const [upvotingId, setUpvotingId] = useState(null);

  useEffect(() => {
    loadFeed();
  }, []);

  const loadFeed = async () => {
    try {
      setLoading(true);
      const data = await api.complaints.getPublicFeed();
      setComplaints(data);
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleUpvote = async (id, e) => {
    e.stopPropagation();
    if (!currentUser || currentUser.role !== 'ROLE_CITIZEN') {
      alert('Only registered citizens can upvote civic complaints. Please switch to a Citizen profile in the top-right menu.');
      return;
    }
    try {
      setUpvotingId(id);
      const updated = await api.complaints.upvote(id);
      setComplaints(prev => prev.map(c => c.id === id ? {
        ...c,
        upvotesCount: updated.upvotesCount,
        priorityScore: updated.priorityScore
      } : c));
    } catch (err) {
      alert(err.message);
    } finally {
      setUpvotingId(null);
    }
  };

  const filtered = complaints.filter(c => {
    if (selectedDept === 'ALL') return true;
    return c.departmentName.toLowerCase().includes(selectedDept.toLowerCase());
  });

  return (
    <div style={{ padding: '2rem 0' }}>

      {/* Hero Banner with Civic Pulse Stats — smart-city illustration background */}
      <div className="glass-panel hero-city-bg" style={{ padding: '2.5rem', marginBottom: '2rem', position: 'relative' }}>
        <div>
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem', background: 'var(--primary-light)', border: '1px solid #bfdbfe', padding: '0.35rem 0.85rem', borderRadius: '999px', fontSize: '0.78rem', fontWeight: 700, color: 'var(--primary)', marginBottom: '1rem' }}>
            <Sparkles size={14} /> TRANSPARENT CIVIC GOVERNANCE
          </div>
          <h1 style={{ fontSize: '2.25rem', fontWeight: 800, letterSpacing: '-0.03em', lineHeight: 1.25, maxWidth: '780px', marginBottom: '0.75rem', color: 'var(--text-main)' }}>
            Dynamic Civic Issue Triage & Prioritization
          </h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '1rem', maxWidth: '680px', marginBottom: '1.5rem', lineHeight: 1.6 }}>
            CivicPulse algorithmically scores complaints based on base hazard severity, verified community upvotes, and waiting-time aging to ensure urgent public issues receive immediate action.
          </p>

          {/* Quick Department Filter Pills */}
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', alignItems: 'center' }}>
            <span style={{ fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-subtle)', display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
              <Filter size={14} /> Filter Dept:
            </span>
            {['ALL', 'Roads', 'Water', 'Solid Waste', 'Electricity'].map(dept => (
              <button
                key={dept}
                onClick={() => setSelectedDept(dept)}
                className={`btn btn-sm ${selectedDept === dept ? 'btn-primary' : 'btn-secondary'}`}
                style={{ borderRadius: '999px', padding: '0.3rem 0.9rem' }}
              >
                {dept}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Issues Feed Grid */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: '4rem', color: 'var(--text-muted)' }}>
          <div className="pulse-beacon" style={{ width: '36px', height: '36px', borderRadius: '50%', background: 'var(--primary)', margin: '0 auto 1rem' }}></div>
          Loading city complaints...
        </div>
      ) : error ? (
        <div className="card-elevated" style={{ borderColor: '#fecaca', background: '#fef2f2', color: '#b91c1c' }}>
          Failed to load feed: {error}
        </div>
      ) : filtered.length === 0 ? (
        <div className="glass-panel" style={{ textAlign: 'center', padding: '3.5rem', color: 'var(--text-muted)' }}>
          No complaints found in this category.
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(360px, 1fr))', gap: '1.5rem' }}>
          {filtered.map(complaint => (
            <div
              key={complaint.id}
              className="card-elevated"
              style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between', cursor: 'pointer' }}
              onClick={() => onTrackComplaint(complaint.trackingNumber)}
            >
              <div>
                {/* Header row: Tracking ID + Severity + Priority Score */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.85rem' }}>
                  <div>
                    <span style={{ fontFamily: 'var(--font-mono)', fontSize: '0.8rem', color: 'var(--primary)', fontWeight: 700, display: 'block' }}>
                      #{complaint.trackingNumber}
                    </span>
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-subtle)' }}>
                      {complaint.departmentName}
                    </span>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <span className={`badge badge-${complaint.severity?.toLowerCase()}`}>
                      {complaint.severity}
                    </span>
                    <div
                      className={`priority-chip ${
                        complaint.priorityScore >= 7 ? 'score-urgent' :
                        complaint.priorityScore >= 5 ? 'score-high' :
                        complaint.priorityScore >= 3 ? 'score-medium' : 'score-low'
                      }`}
                      title="Priority Score = (Severity × 0.45) + (Impact × 0.30) + (Age × 0.25)"
                    >
                      P: {complaint.priorityScore?.toFixed(1)}
                    </div>
                  </div>
                </div>

                {/* Complaint Title */}
                <h3 style={{ fontSize: '1.05rem', fontWeight: 700, color: 'var(--text-main)', marginBottom: '0.5rem', lineHeight: 1.4 }}>
                  {complaint.title}
                </h3>

                {/* Location / Landmark */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: 'var(--text-muted)', fontSize: '0.82rem', marginBottom: '0.75rem' }}>
                  <MapPin size={14} color="#64748b" />
                  <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {complaint.addressLandmark}
                  </span>
                </div>

                {/* Category Pill & Overdue / Escalation Flag */}
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.4rem', alignItems: 'center', marginBottom: '1rem' }}>
                  <span style={{ fontSize: '0.74rem', background: '#f1f5f9', border: '1px solid var(--border-subtle)', padding: '0.2rem 0.5rem', borderRadius: '4px', color: 'var(--text-muted)' }}>
                    {complaint.categoryName}
                  </span>

                  {complaint.isEscalated && (
                    <span style={{ fontSize: '0.74rem', background: '#fef2f2', border: '1px solid #fecaca', color: '#b91c1c', padding: '0.15rem 0.5rem', borderRadius: '4px', display: 'flex', alignItems: 'center', gap: '0.25rem', fontWeight: 700 }}>
                      <ShieldAlert size={12} /> ESCALATED (TIER {complaint.escalationLevel})
                    </span>
                  )}
                </div>
              </div>

              {/* Card Footer: Status + Community Upvotes + Action */}
              <div style={{ borderTop: '1px solid var(--border-subtle)', paddingTop: '0.85rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <span className={`status-pill status-${complaint.status?.toLowerCase().replace(/_/g, '-')}`}>
                  {complaint.status?.replace('_', ' ')}
                </span>

                <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                  {/* Upvote Button */}
                  <button
                    onClick={(e) => handleUpvote(complaint.id, e)}
                    disabled={upvotingId === complaint.id}
                    className="btn btn-sm btn-secondary"
                    style={{
                      borderRadius: '999px',
                      padding: '0.25rem 0.65rem',
                      fontSize: '0.78rem',
                      borderColor: complaint.upvotesCount > 0 ? '#93c5fd' : 'var(--border-medium)',
                      color: complaint.upvotesCount > 0 ? 'var(--primary)' : 'var(--text-muted)',
                      background: complaint.upvotesCount > 0 ? 'var(--primary-light)' : '#ffffff'
                    }}
                    title="Upvote to amplify community impact"
                  >
                    <ThumbsUp size={13} />
                    <span>{complaint.upvotesCount}</span>
                  </button>

                  <span style={{ color: 'var(--primary)', fontSize: '0.8rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.2rem' }}>
                    Track <ArrowRight size={13} />
                  </span>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

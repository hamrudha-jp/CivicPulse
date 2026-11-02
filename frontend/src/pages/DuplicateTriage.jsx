import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { Layers, MapPin, CheckCircle, XCircle, ArrowRight, AlertCircle, Sparkles } from 'lucide-react';

export default function DuplicateTriage({ currentUser }) {
  const [duplicates, setDuplicates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [reviewingId, setReviewingId] = useState(null);

  useEffect(() => {
    loadPendingDuplicates();
  }, []);

  const loadPendingDuplicates = async () => {
    try {
      setLoading(true);
      const data = await api.duplicates.getPending(currentUser?.departmentId);
      setDuplicates(data || []);
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleReview = async (matchId, action) => {
    try {
      setReviewingId(matchId);
      await api.duplicates.review(matchId, action, 'Reviewed via Official Duplicate Triage Console');
      setDuplicates(prev => prev.filter(m => m.id !== matchId));
    } catch (err) {
      alert('Error reviewing duplicate: ' + err.message);
    } finally {
      setReviewingId(null);
    }
  };

  return (
    <div style={{ padding: '2rem 0' }}>

      {/* Header */}
      <div style={{ marginBottom: '2rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.4rem' }}>
          <Layers size={22} color="#2563eb" />
          <h1 style={{ fontSize: '1.85rem', fontWeight: 800 }}>
            Spatial & Lexical Duplicate Triage
          </h1>
        </div>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.92rem' }}>
          The duplicate detection engine identifies incidents within 200m that share significant token overlap. Confirming a merge prevents duplicate work and amplifies the primary issue's priority score.
        </p>
      </div>

      {error && (
        <div className="card-elevated" style={{ borderColor: '#fca5a5', background: '#fef2f2', color: '#b91c1c', marginBottom: '1.5rem' }}>
          {error}
        </div>
      )}

      {loading ? (
        <div style={{ textAlign: 'center', padding: '4rem', color: 'var(--text-muted)' }}>
          <div className="pulse-beacon" style={{ width: '40px', height: '40px', borderRadius: '50%', background: '#2563eb', margin: '0 auto 1rem' }}></div>
          Scanning for candidate duplicates...
        </div>
      ) : duplicates.length === 0 ? (
        <div className="card-elevated" style={{ textAlign: 'center', padding: '3.5rem' }}>
          <CheckCircle size={44} color="#059669" style={{ margin: '0 auto 1rem' }} />
          <h3 style={{ fontSize: '1.3rem', fontWeight: 700, marginBottom: '0.4rem', color: 'var(--text-main)' }}>
            No Pending Duplicate Candidates
          </h3>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
            All spatially clustered citizen reports are currently consolidated. New submissions will automatically appear here if proximity and text confidence exceed 55%.
          </p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.75rem' }}>
          {duplicates.map(match => (
            <div key={match.id} className="card-elevated" style={{ padding: '1.75rem' }}>

              {/* Match Header with Confidence Meter */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '1rem', flexWrap: 'wrap', gap: '0.75rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                  <span style={{ fontSize: '0.8rem', background: '#fef3c7', border: '1px solid #fde68a', color: '#b45309', padding: '0.2rem 0.6rem', borderRadius: '6px', fontWeight: 700 }}>
                    MATCH CANDIDATE #{match.id}
                  </span>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-subtle)' }}>
                    Detected {new Date(match.createdAt).toLocaleTimeString()}
                  </span>
                </div>

                {/* Algorithmic Metrics */}
                <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
                  <div style={{ textAlign: 'right' }}>
                    <div style={{ fontSize: '0.72rem', color: 'var(--text-subtle)' }}>Haversine Distance</div>
                    <div style={{ fontWeight: 700, fontSize: '0.9rem', color: 'var(--primary-600)', fontFamily: 'var(--font-mono)' }}>
                      {match.distanceMeters}m
                    </div>
                  </div>

                  <div style={{ textAlign: 'right' }}>
                    <div style={{ fontSize: '0.72rem', color: 'var(--text-subtle)' }}>Text Similarity</div>
                    <div style={{ fontWeight: 700, fontSize: '0.9rem', color: '#7c3aed', fontFamily: 'var(--font-mono)' }}>
                      {(match.textSimilarityScore * 100).toFixed(0)}%
                    </div>
                  </div>

                  <div style={{
                    background: match.compositeConfidence >= 0.75 ? '#fef2f2' : '#fffbeb',
                    border: `1px solid ${match.compositeConfidence >= 0.75 ? '#fecaca' : '#fde68a'}`,
                    borderRadius: '8px',
                    padding: '0.35rem 0.75rem',
                    textAlign: 'center'
                  }}>
                    <div style={{ fontSize: '0.68rem', color: 'var(--text-subtle)' }}>Confidence</div>
                    <div style={{ fontWeight: 800, fontSize: '1rem', color: match.compositeConfidence >= 0.75 ? '#dc2626' : '#d97706', fontFamily: 'var(--font-mono)' }}>
                      {(match.compositeConfidence * 100).toFixed(0)}%
                    </div>
                  </div>
                </div>
              </div>

              {/* Side-by-side comparison */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr auto 1fr', gap: '1.25rem', alignItems: 'center', marginBottom: '1.5rem' }}>

                {/* Left Card: Source (New) */}
                <div style={{ background: '#ffffff', border: '1px solid var(--border-medium)', borderRadius: '10px', padding: '1.25rem' }}>
                  <span style={{ fontSize: '0.72rem', color: '#d97706', fontWeight: 700, display: 'block', marginBottom: '0.3rem' }}>
                    NEW SUBMISSION
                  </span>
                  <div style={{ fontFamily: 'var(--font-mono)', fontSize: '0.85rem', color: 'var(--primary-600)', fontWeight: 600, marginBottom: '0.35rem' }}>
                    #{match.sourceTrackingNumber}
                  </div>
                  <h4 style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--text-main)', marginBottom: '0.5rem', lineHeight: 1.35 }}>
                    {match.sourceTitle}
                  </h4>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-subtle)', display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
                    <MapPin size={12} /> {match.sourceLat?.toFixed(4)}, {match.sourceLon?.toFixed(4)}
                  </div>
                </div>

                <div style={{ color: 'var(--text-subtle)', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                  <ArrowRight size={20} color="#64748b" />
                  <span style={{ fontSize: '0.68rem', fontWeight: 600, color: 'var(--text-subtle)', marginTop: '0.2rem' }}>MERGE INTO</span>
                </div>

                {/* Right Card: Matched (Existing Primary) */}
                <div style={{ background: '#ffffff', border: '1.5px solid #bfdbfe', borderRadius: '10px', padding: '1.25rem' }}>
                  <span style={{ fontSize: '0.72rem', color: 'var(--primary-600)', fontWeight: 700, display: 'block', marginBottom: '0.3rem' }}>
                    EXISTING ACTIVE INCIDENT
                  </span>
                  <div style={{ fontFamily: 'var(--font-mono)', fontSize: '0.85rem', color: 'var(--primary-600)', fontWeight: 600, marginBottom: '0.35rem' }}>
                    #{match.matchedTrackingNumber}
                  </div>
                  <h4 style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--text-main)', marginBottom: '0.5rem', lineHeight: 1.35 }}>
                    {match.matchedTitle}
                  </h4>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-subtle)', display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
                    <MapPin size={12} /> {match.matchedLat?.toFixed(4)}, {match.matchedLon?.toFixed(4)}
                  </div>
                </div>

              </div>

              {/* Triage Decision Buttons */}
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
                <button
                  onClick={() => handleReview(match.id, 'DISMISSED')}
                  disabled={reviewingId === match.id}
                  className="btn btn-secondary"
                  style={{ fontSize: '0.85rem' }}
                >
                  <XCircle size={15} /> Dismiss (Independent Issue)
                </button>

                <button
                  onClick={() => handleReview(match.id, 'CONFIRMED_MERGED')}
                  disabled={reviewingId === match.id}
                  className="btn btn-primary"
                  style={{ fontSize: '0.85rem' }}
                >
                  <CheckCircle size={15} /> Confirm Merge & Boost Priority
                </button>
              </div>

            </div>
          ))}
        </div>
      )}

    </div>
  );
}

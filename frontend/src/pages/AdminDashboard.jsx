import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { Shield, Activity, AlertTriangle, CheckCircle, Clock, Zap, RefreshCw, BarChart3, Layers } from 'lucide-react';

export default function AdminDashboard() {
  const [metrics, setMetrics] = useState(null);
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [triggeringEscalation, setTriggeringEscalation] = useState(false);
  const [escalationNotice, setEscalationNotice] = useState(null);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const [dash, depts] = await Promise.all([
        api.analytics.getDashboard(),
        api.departments.getAll()
      ]);
      setMetrics(dash);
      setDepartments(depts);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleTriggerEscalation = async () => {
    try {
      setTriggeringEscalation(true);
      const res = await api.admin.triggerEscalation();
      setEscalationNotice('SLA background escalation sweep executed successfully! Breached complaints transitioned to Tier 1 and priority aging updated.');
      loadData();
    } catch (err) {
      alert('Error triggering escalation: ' + err.message);
    } finally {
      setTriggeringEscalation(false);
    }
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '5rem', color: 'var(--text-muted)' }}>
        <div className="pulse-beacon" style={{ width: '40px', height: '40px', borderRadius: '50%', background: 'var(--primary-600)', margin: '0 auto 1rem' }}></div>
        Loading administrative macro metrics...
      </div>
    );
  }

  return (
    <div style={{ padding: '2rem 0' }}>

      {/* Header — panel-city-accent adds a very faint city silhouette to the top-right corner */}
      <div className="panel-city-accent" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.4rem' }}>
            <Shield size={22} color="var(--primary-600)" />
            <h1 style={{ fontSize: '1.85rem', fontWeight: 800 }}>
              Municipal Governance Command Center
            </h1>
          </div>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.92rem' }}>
            High-level oversight of city complaints, SLA compliance rates, department velocity, and automated escalation.
          </p>
        </div>

        <button
          onClick={loadData}
          className="btn btn-secondary"
          style={{ fontSize: '0.82rem' }}
        >
          <RefreshCw size={14} /> Refresh Metrics
        </button>
      </div>

      {escalationNotice && (
        <div style={{
          background: '#ecfdf5',
          border: '1px solid #a7f3d0',
          borderRadius: '10px',
          padding: '1rem',
          color: '#065f46',
          marginBottom: '1.5rem',
          display: 'flex',
          alignItems: 'center',
          gap: '0.5rem'
        }}>
          <CheckCircle size={18} /> {escalationNotice}
        </div>
      )}

      {/* KPI Cards Row */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1.25rem', marginBottom: '2rem' }}>

        {/* Total Complaints */}
        <div className="card-elevated">
          <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-subtle)', marginBottom: '0.5rem' }}>
            <span style={{ fontSize: '0.8rem', fontWeight: 600 }}>TOTAL REGISTERED</span>
            <Activity size={18} color="var(--primary-600)" />
          </div>
          <div style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--text-main)', fontFamily: 'var(--font-mono)' }}>
            {metrics?.totalComplaints || 0}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-subtle)', marginTop: '0.2rem' }}>
            Across all municipal departments
          </div>
        </div>

        {/* Resolution Rate */}
        <div className="card-elevated">
          <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-subtle)', marginBottom: '0.5rem' }}>
            <span style={{ fontSize: '0.8rem', fontWeight: 600 }}>RESOLUTION RATE</span>
            <CheckCircle size={18} color="#059669" />
          </div>
          <div style={{ fontSize: '2rem', fontWeight: 800, color: '#059669', fontFamily: 'var(--font-mono)' }}>
            {metrics?.resolutionRatePercentage || 0}%
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-subtle)', marginTop: '0.2rem' }}>
            {metrics?.resolvedCount || 0} resolved cases verified
          </div>
        </div>

        {/* Overdue SLA Breaches */}
        <div className="card-elevated" style={{ borderColor: metrics?.overdueCount > 0 ? '#fca5a5' : undefined, background: metrics?.overdueCount > 0 ? '#fff1f2' : undefined }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-subtle)', marginBottom: '0.5rem' }}>
            <span style={{ fontSize: '0.8rem', fontWeight: 600 }}>OVERDUE BREACHES</span>
            <AlertTriangle size={18} color="#e11d48" />
          </div>
          <div style={{ fontSize: '2rem', fontWeight: 800, color: '#be123c', fontFamily: 'var(--font-mono)' }}>
            {metrics?.overdueCount || 0}
          </div>
          <div style={{ fontSize: '0.75rem', color: '#be123c', marginTop: '0.2rem' }}>
            {metrics?.overdueCount > 0 ? 'Exceeded category resolution SLA' : 'All issues on schedule'}
          </div>
        </div>

        {/* Pending Duplicates */}
        <div className="card-elevated">
          <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-subtle)', marginBottom: '0.5rem' }}>
            <span style={{ fontSize: '0.8rem', fontWeight: 600 }}>DUPLICATE CANDIDATES</span>
            <Layers size={18} color="#d97706" />
          </div>
          <div style={{ fontSize: '2rem', fontWeight: 800, color: '#b45309', fontFamily: 'var(--font-mono)' }}>
            {metrics?.pendingDuplicateReviewsCount || 0}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-subtle)', marginTop: '0.2rem' }}>
            Awaiting official merge triage
          </div>
        </div>

      </div>

      {/* Two Column Layout: Distributions + Escalation Controls */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '1.75rem', marginBottom: '2rem' }}>

        {/* Severity Distribution */}
        <div className="card-elevated" style={{ padding: '1.75rem' }}>
          <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '1.25rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <BarChart3 size={18} color="var(--primary-600)" /> Complaints by Hazard Severity
          </h3>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {[
              { key: 'CRITICAL', label: 'Critical (10 pts base)', color: '#ef4444' },
              { key: 'HIGH', label: 'High (7 pts base)', color: '#f97316' },
              { key: 'MEDIUM', label: 'Medium (4 pts base)', color: '#eab308' },
              { key: 'LOW', label: 'Low (2 pts base)', color: '#10b981' },
            ].map(s => {
              const count = metrics?.complaintsBySeverity?.[s.key] || 0;
              const pct = metrics?.totalComplaints > 0 ? Math.round((count / metrics.totalComplaints) * 100) : 0;
              return (
                <div key={s.key}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '0.35rem' }}>
                    <span style={{ fontWeight: 600, color: s.color }}>{s.label}</span>
                    <span style={{ fontFamily: 'var(--font-mono)', color: 'var(--text-muted)' }}>{count} ({pct}%)</span>
                  </div>
                  <div style={{ height: '8px', background: 'var(--border-subtle)', borderRadius: '4px', overflow: 'hidden' }}>
                    <div style={{ width: `${pct}%`, height: '100%', background: s.color, borderRadius: '4px', transition: 'width 0.4s ease' }}></div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Automated SLA Escalation Engine Panel */}
        <div className="card-elevated" style={{ padding: '1.75rem', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
              <Zap size={18} color="#d97706" />
              <h3 style={{ fontSize: '1.1rem', fontWeight: 700 }}>
                Automated SLA & Escalation Engine
              </h3>
            </div>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem', lineHeight: 1.5, marginBottom: '1.25rem' }}>
              Spring Boot executes an automated scheduled cron (<code style={{ color: 'var(--primary-700)', background: 'var(--bg-subtle)', padding: '0.1rem 0.35rem', borderRadius: '4px' }}>@Scheduled</code>) every 15 minutes. It sweeps the database for cases where <code style={{ color: 'var(--primary-700)', background: 'var(--bg-subtle)', padding: '0.1rem 0.35rem', borderRadius: '4px' }}>now &gt; dueDate</code>, transitions them to <code style={{ color: '#be123c', background: '#ffe4e6', padding: '0.1rem 0.35rem', borderRadius: '4px' }}>isEscalated = true</code>, and applies dynamic aging penalties to active priorities.
            </p>

            <div style={{ background: 'var(--bg-subtle)', padding: '1rem', borderRadius: '8px', border: '1px solid var(--border-subtle)', marginBottom: '1.5rem' }}>
              <div style={{ fontSize: '0.78rem', color: 'var(--text-subtle)', marginBottom: '0.3rem' }}>ACTIVE SCHEDULE CONFIGURATION</div>
              <div style={{ fontFamily: 'var(--font-mono)', fontSize: '0.9rem', color: '#059669', fontWeight: 600 }}>
                Cron: "0 */15 * * * *" (Every 15 minutes)
              </div>
            </div>
          </div>

          <button
            onClick={handleTriggerEscalation}
            disabled={triggeringEscalation}
            className="btn btn-primary"
            style={{ width: '100%', padding: '0.8rem', background: '#d97706', borderColor: '#d97706', color: '#ffffff' }}
          >
            {triggeringEscalation ? 'Sweeping SLA Deadlines...' : (
              <>
                <Zap size={16} /> Trigger Immediate SLA Escalation Sweep
              </>
            )}
          </button>
        </div>

      </div>

      {/* Department SLA Configuration Table */}
      <div className="card-elevated" style={{ padding: '1.75rem' }}>
        <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '1rem' }}>
          Municipal Department & Category SLA Matrix
        </h3>

        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.85rem' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid var(--border-medium)', background: 'var(--bg-subtle)' }}>
                <th style={{ padding: '0.75rem', color: 'var(--text-subtle)' }}>Department</th>
                <th style={{ padding: '0.75rem', color: 'var(--text-subtle)' }}>Contact Email</th>
                <th style={{ padding: '0.75rem', color: 'var(--text-subtle)' }}>Configured Categories</th>
              </tr>
            </thead>
            <tbody>
              {departments.map(dept => (
                <tr key={dept.id} style={{ borderBottom: '1px solid var(--border-subtle)' }}>
                  <td style={{ padding: '0.75rem', fontWeight: 700, color: 'var(--text-main)' }}>
                    {dept.name}
                  </td>
                  <td style={{ padding: '0.75rem', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
                    {dept.contactEmail}
                  </td>
                  <td style={{ padding: '0.75rem' }}>
                    <div style={{ display: 'flex', gap: '0.4rem', flexWrap: 'wrap' }}>
                      {dept.categories?.map(c => (
                        <span key={c.id} style={{ background: 'var(--bg-subtle)', padding: '0.2rem 0.6rem', borderRadius: '6px', fontSize: '0.75rem', border: '1px solid var(--border-subtle)', color: 'var(--text-main)' }}>
                          {c.name} (<strong>{c.slaHours}h</strong>)
                        </span>
                      ))}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

    </div>
  );
}

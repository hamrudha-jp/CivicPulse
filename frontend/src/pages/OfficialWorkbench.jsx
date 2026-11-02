import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { Building, Filter, CheckCircle, Clock, ShieldAlert, UserCheck, ArrowUpDown, ChevronRight, X, AlertTriangle } from 'lucide-react';

export default function OfficialWorkbench({ currentUser, onTrackComplaint }) {
  const [queue, setQueue] = useState([]);
  const [officials, setOfficials] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Filters
  const [selectedStatus, setSelectedStatus] = useState('');
  const [selectedSeverity, setSelectedSeverity] = useState('');
  const [filterEscalated, setFilterEscalated] = useState('');

  // Selected complaint for modal action
  const [activeComplaint, setActiveComplaint] = useState(null);
  const [modalMode, setModalMode] = useState(''); // 'assign', 'status', 'resolve'
  const [assignOfficialId, setAssignOfficialId] = useState('');
  const [statusUpdate, setStatusUpdate] = useState('IN_PROGRESS');
  const [actionRemarks, setActionRemarks] = useState('');
  const [actionLoading, setActionLoading] = useState(false);

  useEffect(() => {
    loadQueue();
    loadOfficials();
  }, [selectedStatus, selectedSeverity, filterEscalated]);

  const loadQueue = async () => {
    try {
      setLoading(true);
      const params = {};
      if (selectedStatus) params.status = selectedStatus;
      if (selectedSeverity) params.severity = selectedSeverity;
      if (filterEscalated) params.isEscalated = filterEscalated;
      if (currentUser?.departmentId) params.departmentId = currentUser.departmentId;

      const data = await api.official.getQueue(params);
      setQueue(data.content || []);
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const loadOfficials = async () => {
    try {
      const data = await api.official.getAssignees(currentUser?.departmentId);
      setOfficials(data || []);
      if (data && data.length > 0) {
        setAssignOfficialId(data[0].id.toString());
      }
    } catch (err) {
      console.error(err);
    }
  };

  const handleAssign = async () => {
    if (!assignOfficialId) return;
    try {
      setActionLoading(true);
      await api.official.assign(activeComplaint.id, parseInt(assignOfficialId, 10));
      closeModal();
      loadQueue();
    } catch (err) {
      alert('Failed to assign: ' + err.message);
    } finally {
      setActionLoading(false);
    }
  };

  const handleUpdateStatus = async () => {
    try {
      setActionLoading(true);
      await api.official.updateStatus(activeComplaint.id, statusUpdate, actionRemarks);
      closeModal();
      loadQueue();
    } catch (err) {
      alert('Failed to update status: ' + err.message);
    } finally {
      setActionLoading(false);
    }
  };

  const handleResolve = async () => {
    if (!actionRemarks.trim()) {
      alert('Please enter resolution remarks.');
      return;
    }
    try {
      setActionLoading(true);
      await api.official.resolveJson(activeComplaint.id, actionRemarks);
      closeModal();
      loadQueue();
    } catch (err) {
      alert('Failed to resolve: ' + err.message);
    } finally {
      setActionLoading(false);
    }
  };

  const closeModal = () => {
    setActiveComplaint(null);
    setModalMode('');
    setActionRemarks('');
  };

  return (
    <div style={{ padding: '2rem 0' }}>

      {/* Header — subtle city silhouette accent in top-right corner */}
      <div className="panel-city-accent" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem', marginBottom: '2rem' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.4rem' }}>
            <Building size={20} color="#2563eb" />
            <h1 style={{ fontSize: '1.85rem', fontWeight: 800 }}>
              Departmental Operational Queue
            </h1>
          </div>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.92rem' }}>
            Issues are ranked in descending order by the mathematical Priority Score ($S \times 0.45 + I \times 0.30 + A \times 0.25$).
          </p>
        </div>

        {/* Filter Bar */}
        <div style={{ display: 'flex', gap: '0.6rem', flexWrap: 'wrap', alignItems: 'center' }}>
          <select
            className="form-select"
            style={{ width: 'auto', padding: '0.4rem 0.8rem', fontSize: '0.8rem' }}
            value={selectedStatus}
            onChange={(e) => setSelectedStatus(e.target.value)}
          >
            <option value="">All Statuses</option>
            <option value="SUBMITTED">Submitted</option>
            <option value="ASSIGNED">Assigned</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="RESOLVED">Resolved</option>
            <option value="DUPLICATE">Duplicate</option>
          </select>

          <select
            className="form-select"
            style={{ width: 'auto', padding: '0.4rem 0.8rem', fontSize: '0.8rem' }}
            value={selectedSeverity}
            onChange={(e) => setSelectedSeverity(e.target.value)}
          >
            <option value="">All Severities</option>
            <option value="CRITICAL">Critical</option>
            <option value="HIGH">High</option>
            <option value="MEDIUM">Medium</option>
            <option value="LOW">Low</option>
          </select>

          <select
            className="form-select"
            style={{ width: 'auto', padding: '0.4rem 0.8rem', fontSize: '0.8rem' }}
            value={filterEscalated}
            onChange={(e) => setFilterEscalated(e.target.value)}
          >
            <option value="">All Deadlines</option>
            <option value="true">Escalated Only (Breached SLA)</option>
            <option value="false">Within SLA</option>
          </select>
        </div>
      </div>

      {error && (
        <div className="card-elevated" style={{ borderColor: '#fca5a5', background: '#fef2f2', color: '#b91c1c', marginBottom: '1.5rem' }}>
          {error}
        </div>
      )}

      {/* Queue Table */}
      <div className="card-elevated" style={{ padding: 0, overflow: 'hidden' }}>
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.88rem' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid var(--border-medium)', background: 'var(--bg-subtle)' }}>
                <th style={{ padding: '1rem', color: 'var(--text-subtle)', fontWeight: 700 }}>Priority</th>
                <th style={{ padding: '1rem', color: 'var(--text-subtle)', fontWeight: 700 }}>Tracking ID</th>
                <th style={{ padding: '1rem', color: 'var(--text-subtle)', fontWeight: 700 }}>Issue Title & Dept</th>
                <th style={{ padding: '1rem', color: 'var(--text-subtle)', fontWeight: 700 }}>Severity</th>
                <th style={{ padding: '1rem', color: 'var(--text-subtle)', fontWeight: 700 }}>Status</th>
                <th style={{ padding: '1rem', color: 'var(--text-subtle)', fontWeight: 700 }}>SLA Due Date</th>
                <th style={{ padding: '1rem', color: 'var(--text-subtle)', fontWeight: 700, textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-muted)' }}>
                    Loading priority queue...
                  </td>
                </tr>
              ) : queue.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-muted)' }}>
                    No complaints match current filters.
                  </td>
                </tr>
              ) : (
                queue.map(item => (
                  <tr
                    key={item.id}
                    style={{ borderBottom: '1px solid var(--border-subtle)', transition: 'background 0.15s ease' }}
                    onMouseEnter={(e) => e.currentTarget.style.background = 'var(--bg-subtle)'}
                    onMouseLeave={(e) => e.currentTarget.style.background = 'transparent'}
                  >
                    {/* Priority Score Chip */}
                    <td style={{ padding: '1rem' }}>
                      <div className={`priority-chip ${
                        item.priorityScore >= 7 ? 'score-urgent' :
                        item.priorityScore >= 5 ? 'score-high' :
                        item.priorityScore >= 3 ? 'score-medium' : 'score-low'
                      }`}>
                        {item.priorityScore?.toFixed(2)}
                      </div>
                    </td>

                    {/* Tracking ID */}
                    <td style={{ padding: '1rem', fontFamily: 'var(--font-mono)', fontWeight: 600, color: 'var(--primary-600)' }}>
                      <span style={{ cursor: 'pointer', textDecoration: 'underline' }} onClick={() => onTrackComplaint(item.trackingNumber)}>
                        #{item.trackingNumber}
                      </span>
                    </td>

                    {/* Title & Category */}
                    <td style={{ padding: '1rem', maxWidth: '320px' }}>
                      <div style={{ fontWeight: 600, color: 'var(--text-main)', marginBottom: '0.2rem', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {item.title}
                      </div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-subtle)' }}>
                        {item.categoryName} • {item.addressLandmark}
                      </div>
                    </td>

                    {/* Severity */}
                    <td style={{ padding: '1rem' }}>
                      <span className={`badge badge-${item.severity?.toLowerCase()}`}>
                        {item.severity}
                      </span>
                    </td>

                    {/* Status */}
                    <td style={{ padding: '1rem' }}>
                      <span className={`status-pill status-${item.status?.toLowerCase().replace('_', '')}`}>
                        {item.status?.replace('_', ' ')}
                      </span>
                    </td>

                    {/* SLA Due Date / Escalation Flag */}
                    <td style={{ padding: '1rem' }}>
                      {item.isEscalated ? (
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', color: '#f87171', fontWeight: 700, fontSize: '0.78rem' }}>
                          <ShieldAlert size={14} /> ESCALATED (TIER {item.escalationLevel})
                        </div>
                      ) : (
                        <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
                          <Clock size={13} /> {new Date(item.dueDate).toLocaleDateString()}
                        </div>
                      )}
                    </td>

                    {/* Action Buttons */}
                    <td style={{ padding: '1rem', textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', gap: '0.4rem' }}>
                        <button
                          onClick={() => { setActiveComplaint(item); setModalMode('assign'); }}
                          className="btn btn-sm btn-secondary"
                          title="Assign to official"
                        >
                          Assign
                        </button>

                        <button
                          onClick={() => { setActiveComplaint(item); setModalMode('status'); }}
                          className="btn btn-sm btn-secondary"
                          title="Update operational status"
                        >
                          Status
                        </button>

                        {item.status !== 'RESOLVED' && (
                          <button
                            onClick={() => { setActiveComplaint(item); setModalMode('resolve'); }}
                            className="btn btn-sm btn-success"
                            title="Resolve complaint"
                          >
                            Resolve
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal Actions */}
      {activeComplaint && (
        <div style={{
          position: 'fixed',
          inset: 0,
          background: 'rgba(15, 23, 42, 0.45)',
          backdropFilter: 'blur(4px)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 200,
          padding: '1rem'
        }}>
          <div className="card-elevated" style={{ width: '100%', maxWidth: '520px', padding: '2rem', boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
              <h3 style={{ fontSize: '1.25rem', fontWeight: 800 }}>
                {modalMode === 'assign' && 'Assign Complaint to Official'}
                {modalMode === 'status' && 'Update Operational Status'}
                {modalMode === 'resolve' && 'Mark Issue as Resolved'}
              </h3>
              <button onClick={closeModal} style={{ background: 'none', border: 'none', color: 'var(--text-subtle)', cursor: 'pointer' }}>
                <X size={20} />
              </button>
            </div>

            <div style={{ background: 'var(--bg-subtle)', border: '1px solid var(--border-subtle)', padding: '0.85rem 1rem', borderRadius: '8px', marginBottom: '1.25rem' }}>
              <div style={{ fontFamily: 'var(--font-mono)', fontSize: '0.82rem', color: 'var(--primary-600)', fontWeight: 600 }}>
                #{activeComplaint.trackingNumber}
              </div>
              <div style={{ fontWeight: 600, fontSize: '0.9rem', color: 'var(--text-main)', marginTop: '0.2rem' }}>
                {activeComplaint.title}
              </div>
            </div>

            {/* Mode: Assign */}
            {modalMode === 'assign' && (
              <div>
                <div className="form-group">
                  <label className="form-label">Select Field Official / Engineer</label>
                  <select
                    className="form-select"
                    value={assignOfficialId}
                    onChange={(e) => setAssignOfficialId(e.target.value)}
                  >
                    {officials.map(off => (
                      <option key={off.id} value={off.id}>
                        {off.fullName} ({off.email})
                      </option>
                    ))}
                  </select>
                </div>
                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.5rem' }}>
                  <button onClick={closeModal} className="btn btn-secondary">Cancel</button>
                  <button onClick={handleAssign} disabled={actionLoading} className="btn btn-primary">
                    {actionLoading ? 'Assigning...' : 'Confirm Assignment'}
                  </button>
                </div>
              </div>
            )}

            {/* Mode: Update Status */}
            {modalMode === 'status' && (
              <div>
                <div className="form-group">
                  <label className="form-label">New Status</label>
                  <select
                    className="form-select"
                    value={statusUpdate}
                    onChange={(e) => setStatusUpdate(e.target.value)}
                  >
                    <option value="UNDER_REVIEW">Under Review</option>
                    <option value="ASSIGNED">Assigned</option>
                    <option value="IN_PROGRESS">In Progress</option>
                    <option value="REJECTED">Rejected</option>
                  </select>
                </div>

                <div className="form-group">
                  <label className="form-label">Operational Remarks / Action Log</label>
                  <textarea
                    className="form-textarea"
                    rows={3}
                    placeholder="e.g. Field crew dispatched with asphalt repair kit"
                    value={actionRemarks}
                    onChange={(e) => setActionRemarks(e.target.value)}
                  />
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.5rem' }}>
                  <button onClick={closeModal} className="btn btn-secondary">Cancel</button>
                  <button onClick={handleUpdateStatus} disabled={actionLoading} className="btn btn-primary">
                    {actionLoading ? 'Updating...' : 'Save Status'}
                  </button>
                </div>
              </div>
            )}

            {/* Mode: Resolve */}
            {modalMode === 'resolve' && (
              <div>
                <div className="form-group">
                  <label className="form-label">Official Resolution Remarks *</label>
                  <textarea
                    className="form-textarea"
                    rows={3}
                    placeholder="Describe how the issue was resolved (e.g. Pothole filled and sealed with bitumen emulsion; road clear)..."
                    value={actionRemarks}
                    onChange={(e) => setActionRemarks(e.target.value)}
                    required
                  />
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.5rem' }}>
                  <button onClick={closeModal} className="btn btn-secondary">Cancel</button>
                  <button onClick={handleResolve} disabled={actionLoading} className="btn btn-success">
                    {actionLoading ? 'Completing...' : 'Mark as Resolved'}
                  </button>
                </div>
              </div>
            )}

          </div>
        </div>
      )}

    </div>
  );
}

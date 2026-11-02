import React from 'react';
import { Shield, Activity, Building, Layers, ShieldAlert } from 'lucide-react';

export default function Navbar({ activeTab, setActiveTab, currentUser, onSwitchUser }) {
  const demoAccounts = [
    { label: 'Citizen: Arun Kumar', email: 'arun.kumar@gmail.com', pass: 'citizen123', role: 'ROLE_CITIZEN' },
    { label: 'Official: Roads (Rajesh)', email: 'roads.officer@civicpulse.gov', pass: 'official123', role: 'ROLE_OFFICIAL' },
    { label: 'Official: Water (Pooja)', email: 'water.officer@civicpulse.gov', pass: 'official123', role: 'ROLE_OFFICIAL' },
    { label: 'Admin: Chief Officer', email: 'admin@civicpulse.gov', pass: 'admin123', role: 'ROLE_ADMIN' },
  ];

  return (
    <header style={{
      borderBottom: '1px solid var(--border-subtle)',
      background: '#ffffff',
      position: 'sticky',
      top: 0,
      zIndex: 100,
      boxShadow: 'var(--shadow-card)'
    }}>
      <div className="container" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', height: '72px' }}>

        {/* Brand Logo */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.85rem', cursor: 'pointer' }} onClick={() => setActiveTab(currentUser?.role === 'ROLE_OFFICIAL' ? 'official' : 'feed')}>
          <div style={{
            width: '40px',
            height: '40px',
            borderRadius: '10px',
            background: 'var(--primary)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 2px 6px rgba(37, 99, 235, 0.25)'
          }}>
            <Shield size={22} color="#ffffff" />
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <span style={{ fontSize: '1.25rem', fontWeight: 800, letterSpacing: '-0.02em', color: 'var(--text-main)' }}>
                Civic<span style={{ color: 'var(--primary)' }}>Pulse</span>
              </span>
              <span className="pulse-beacon" style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#16a34a' }}></span>
            </div>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-subtle)', fontWeight: 600, letterSpacing: '0.05em' }}>
              MUNICIPAL PRIORITIZATION ENGINE
            </div>
          </div>
        </div>

        {/* Navigation Tabs */}
        <nav style={{ display: 'flex', gap: '0.35rem', background: '#f1f5f9', padding: '4px', borderRadius: '10px', border: '1px solid var(--border-subtle)' }}>
          <button
            onClick={() => setActiveTab('feed')}
            className={`btn btn-sm ${activeTab === 'feed' ? 'btn-primary' : 'btn-secondary'}`}
            style={{ border: 'none' }}
          >
            <Activity size={14} /> City Feed
          </button>

          {/* Citizen-Only Submission View */}
          {(!currentUser || currentUser?.role === 'ROLE_CITIZEN') && (
            <button
              onClick={() => setActiveTab('submit')}
              className={`btn btn-sm ${activeTab === 'submit' ? 'btn-primary' : 'btn-secondary'}`}
              style={{ border: 'none' }}
            >
              Submit Issue
            </button>
          )}

          <button
            onClick={() => setActiveTab('track')}
            className={`btn btn-sm ${activeTab === 'track' ? 'btn-primary' : 'btn-secondary'}`}
            style={{ border: 'none' }}
          >
            Track Status
          </button>

          {/* Official Workbench Tab (Available to Officials and Admins) */}
          {(currentUser?.role === 'ROLE_OFFICIAL' || currentUser?.role === 'ROLE_ADMIN') && (
            <button
              onClick={() => setActiveTab('official')}
              className={`btn btn-sm ${activeTab === 'official' ? 'btn-primary' : 'btn-secondary'}`}
              style={{
                border: 'none',
                background: activeTab === 'official' ? 'var(--primary)' : undefined,
                color: activeTab === 'official' ? '#ffffff' : undefined
              }}
            >
              <Building size={14} /> Official Queue
            </button>
          )}

          {/* Duplicate Triage Tab */}
          {(currentUser?.role === 'ROLE_OFFICIAL' || currentUser?.role === 'ROLE_ADMIN') && (
            <button
              onClick={() => setActiveTab('duplicates')}
              className={`btn btn-sm ${activeTab === 'duplicates' ? 'btn-primary' : 'btn-secondary'}`}
              style={{
                border: 'none',
                background: activeTab === 'duplicates' ? '#d97706' : undefined,
                color: activeTab === 'duplicates' ? '#ffffff' : undefined
              }}
            >
              <Layers size={14} /> Duplicate Triage
            </button>
          )}

          {/* Admin Dashboard */}
          {currentUser?.role === 'ROLE_ADMIN' && (
            <button
              onClick={() => setActiveTab('admin')}
              className={`btn btn-sm ${activeTab === 'admin' ? 'btn-primary' : 'btn-secondary'}`}
              style={{
                border: 'none',
                background: activeTab === 'admin' ? '#dc2626' : undefined,
                color: activeTab === 'admin' ? '#ffffff' : undefined
              }}
            >
              <ShieldAlert size={14} /> Admin Metrics
            </button>
          )}
        </nav>

        {/* User Role Switcher & Profile Badge */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end' }}>
            <span style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-main)' }}>
              {currentUser ? currentUser.fullName : 'Guest Visitor'}
            </span>
            <span style={{ fontSize: '0.72rem', color: 'var(--primary)', fontWeight: 600 }}>
              {currentUser ? currentUser.role.replace('ROLE_', '') : 'Read-Only'}
              {currentUser?.departmentName ? ` • ${currentUser.departmentName}` : ''}
            </span>
          </div>

          <div style={{ position: 'relative' }}>
            <select
              style={{
                background: '#ffffff',
                color: 'var(--text-main)',
                border: '1px solid var(--border-medium)',
                borderRadius: '8px',
                padding: '0.45rem 0.65rem',
                fontSize: '0.78rem',
                fontWeight: 600,
                cursor: 'pointer',
                boxShadow: 'var(--shadow-card)'
              }}
              value={currentUser?.email || ''}
              onChange={(e) => {
                const selected = demoAccounts.find(a => a.email === e.target.value);
                if (selected) {
                  onSwitchUser(selected.email, selected.pass);
                }
              }}
            >
              <option value="" disabled>Switch Role (Demo)...</option>
              {demoAccounts.map(acc => (
                <option key={acc.email} value={acc.email}>{acc.label}</option>
              ))}
            </select>
          </div>
        </div>

      </div>
    </header>
  );
}

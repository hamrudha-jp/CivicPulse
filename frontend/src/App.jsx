import React, { useState, useEffect } from 'react';
import Navbar from './components/Navbar';
import PublicFeed from './pages/PublicFeed';
import SubmitComplaint from './pages/SubmitComplaint';
import TrackComplaint from './pages/TrackComplaint';
import OfficialWorkbench from './pages/OfficialWorkbench';
import DuplicateTriage from './pages/DuplicateTriage';
import AdminDashboard from './pages/AdminDashboard';
import { api, getStoredUser } from './services/api';

export default function App() {
  const [currentUser, setCurrentUser] = useState(null);
  const [activeTab, setActiveTab] = useState('feed');
  const [selectedTrackingNumber, setSelectedTrackingNumber] = useState('CP-2026-R0001');

  useEffect(() => {
    // Check if user already stored or login with default citizen Arun Kumar for immediate interactive demo
    const existing = getStoredUser();
    if (existing) {
      setCurrentUser(existing);
    } else {
      // Auto authenticate as Citizen Arun Kumar so reviewer can immediately submit & upvote
      switchUser('arun.kumar@gmail.com', 'citizen123');
    }
  }, []);

  const switchUser = async (email, password) => {
    try {
      const user = await api.auth.login(email, password);
      setCurrentUser(user);

      // Role-specific landing tab alignment
      if (user.role === 'ROLE_OFFICIAL') {
        setActiveTab('official');
      } else if (user.role === 'ROLE_ADMIN') {
        setActiveTab('admin');
      } else if (user.role === 'ROLE_CITIZEN') {
        if (activeTab === 'official' || activeTab === 'duplicates' || activeTab === 'admin') {
          setActiveTab('feed');
        }
      }
    } catch (err) {
      console.error('Failed to authenticate:', err);
    }
  };

  const handleTrackComplaint = (trackingNum) => {
    setSelectedTrackingNumber(trackingNum);
    setActiveTab('track');
  };

  const handleSubmissionSuccess = (trackingNum) => {
    setSelectedTrackingNumber(trackingNum);
    setActiveTab('track');
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', backgroundColor: 'var(--bg-app)' }}>

      {/* Top Navigation */}
      <Navbar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        currentUser={currentUser}
        onSwitchUser={switchUser}
      />

      {/* Main Content View */}
      <main className="container" style={{ flex: 1, paddingBottom: '3rem' }}>
        {activeTab === 'feed' && (
          <PublicFeed
            onTrackComplaint={handleTrackComplaint}
            currentUser={currentUser}
          />
        )}

        {activeTab === 'submit' && currentUser?.role !== 'ROLE_OFFICIAL' && (
          <SubmitComplaint
            currentUser={currentUser}
            onSubmissionSuccess={handleSubmissionSuccess}
          />
        )}

        {activeTab === 'track' && (
          <TrackComplaint
            initialTrackingNumber={selectedTrackingNumber}
            currentUser={currentUser}
          />
        )}

        {activeTab === 'official' && (currentUser?.role === 'ROLE_OFFICIAL' || currentUser?.role === 'ROLE_ADMIN') && (
          <OfficialWorkbench
            currentUser={currentUser}
            onTrackComplaint={handleTrackComplaint}
          />
        )}

        {activeTab === 'duplicates' && (currentUser?.role === 'ROLE_OFFICIAL' || currentUser?.role === 'ROLE_ADMIN') && (
          <DuplicateTriage
            currentUser={currentUser}
          />
        )}

        {activeTab === 'admin' && currentUser?.role === 'ROLE_ADMIN' && (
          <AdminDashboard />
        )}
      </main>

      {/* Clean Municipal Light Footer */}
      <footer style={{
        borderTop: '1px solid var(--border-subtle)',
        background: '#ffffff',
        padding: '1.75rem 0',
        marginTop: 'auto'
      }}>
        <div className="container" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem', fontSize: '0.85rem', color: 'var(--text-subtle)' }}>
          <div>
            <strong style={{ color: 'var(--text-main)' }}>CivicPulse</strong> — Smart Public Complaint Prioritization Platform
            <div style={{ marginTop: '0.2rem', color: 'var(--text-muted)', fontSize: '0.8rem' }}>
              Built with Java 21 LTS, Spring Boot 3.3.5, PostgreSQL 17, and React + Vite.
            </div>
          </div>

          <div style={{ display: 'flex', gap: '1.25rem', fontFamily: 'var(--font-mono)', fontSize: '0.78rem', color: 'var(--text-muted)' }}>
            <span>Backend: Spring Boot 3 (Port 8080)</span>
            <span>Frontend: React + Vite (Port 5173)</span>
            <span>DB: PostgreSQL (Port 5432)</span>
          </div>
        </div>
      </footer>

    </div>
  );
}

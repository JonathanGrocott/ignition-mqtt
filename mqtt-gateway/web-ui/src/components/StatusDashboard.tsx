import React, { useState, useEffect } from 'react';
import { getModuleStatus } from '../api';
import { ModuleStatus } from '../types';

const StatusDashboard: React.FC = () => {
    const [status, setStatus] = useState<ModuleStatus | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [autoRefresh, setAutoRefresh] = useState(true);

    useEffect(() => {
        loadStatus();

        if (autoRefresh) {
            const interval = setInterval(loadStatus, 5000); // Refresh every 5 seconds
            return () => clearInterval(interval);
        }
    }, [autoRefresh]);

    const loadStatus = async () => {
        try {
            const response = await getModuleStatus();

            if (response.success && response.data) {
                console.log('Status response:', response.data);
                console.log('Statistics:', response.data.statistics);
                setStatus(response.data);
                setError(null);
            } else {
                throw new Error(response.error || 'Failed to load status');
            }
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to load status');
        } finally {
            setLoading(false);
        }
    };

    const getHealthBadgeClass = (healthLevel?: string) => {
        switch (healthLevel) {
            case 'HEALTHY':
                return 'badge-success';
            case 'DEGRADED':
                return 'badge-warning';
            case 'UNHEALTHY':
                return 'badge-error';
            default:
                return 'badge-unknown';
        }
    };

    const getConnectionBadgeClass = (connectionState?: string) => {
        switch (connectionState) {
            case 'CONNECTED':
                return 'badge-success';
            case 'CONNECTING':
            case 'RECONNECTING':
                return 'badge-warning';
            case 'DISCONNECTED':
            case 'ERROR':
                return 'badge-error';
            default:
                return 'badge-unknown';
        }
    };

    if (loading) {
        return (
            <div className="status-dashboard">
                <div className="loading">Loading status...</div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="status-dashboard">
                <div className="error-banner">
                    <strong>Error:</strong> {error}
                    <button onClick={loadStatus} className="btn-retry">Retry</button>
                </div>
            </div>
        );
    }

    if (!status) {
        return null;
    }

    return (
        <div className="status-dashboard">
            <div className="status-header">
                <h2>Module Status</h2>
                <label className="auto-refresh">
                    <input
                        type="checkbox"
                        checked={autoRefresh}
                        onChange={(e) => setAutoRefresh(e.target.checked)}
                    />
                    Auto-refresh (5s)
                </label>
            </div>

            <div className="status-grid">
                {/* Health Status */}
                <div className="status-card">
                    <h3>Health Status</h3>
                    <div className="status-content">
                        <div className={`badge ${getHealthBadgeClass(status.healthLevel)}`}>
                            {status.healthLevel || 'UNKNOWN'}
                        </div>
                        <p className="status-message">{status.statusMessage}</p>
                    </div>
                </div>

                {/* Connection Status */}
                <div className="status-card">
                    <h3>MQTT Brokers</h3>
                    <div className="status-content">
                        <div className={`badge ${getConnectionBadgeClass(status.connectionState)}`}>
                            {status.connectionStateDisplay || status.connectionState || 'UNKNOWN'}
                        </div>
                        {status.totalBrokers === 0 ? (
                            <p className="broker-counts">No brokers configured</p>
                        ) : status.activeBrokers === 0 ? (
                            <>
                                <p className="broker-counts">
                                    {status.totalBrokers} available, 0 in use
                                </p>
                                <p className="hint-text">
                                    Create topic mappings in <strong>Tag Selection</strong> to activate
                                </p>
                            </>
                        ) : (
                            <p className="broker-counts">
                                {status.activeBrokers} connected / {status.totalBrokers} available
                            </p>
                        )}
                    </div>
                </div>

                {/* Tag Monitoring */}
                <div className="status-card">
                    <h3>Tag Monitoring</h3>
                    <div className="status-content">
                        <div className="stat-value">{status.monitoredTagCount}</div>
                        <p className="stat-label">Monitored Tags</p>
                    </div>
                </div>

                {/* Uptime */}
                <div className="status-card">
                    <h3>Uptime</h3>
                    <div className="status-content">
                        <div className="stat-value">
                            {status.statistics?.uptimeDisplay || 'N/A'}
                        </div>
                        <p className="stat-label">
                            {status.statistics?.uptimeMs?.toLocaleString() || '0'} ms
                        </p>
                    </div>
                </div>
            </div>

            <div className="statistics-section">
                <h3>Publishing Statistics</h3>
                <div className="stats-grid">
                    <div className="stat-item">
                        <label>Messages Published</label>
                        <span className="stat-value">
                            {status.statistics?.messagesPublished?.toLocaleString() || '0'}
                        </span>
                    </div>

                    <div className="stat-item">
                        <label>Messages Failed</label>
                        <span className="stat-value">
                            {status.statistics?.messagesFailed?.toLocaleString() || '0'}
                        </span>
                    </div>

                    <div className="stat-item">
                        <label>Publish Success Rate</label>
                        <span className="stat-value">
                            {status.statistics?.publishSuccessRate?.toFixed(1) || '0'}%
                        </span>
                    </div>

                    <div className="stat-item">
                        <label>Tag Reads Successful</label>
                        <span className="stat-value">
                            {status.statistics?.tagReadsSuccessful?.toLocaleString() || '0'}
                        </span>
                    </div>

                    <div className="stat-item">
                        <label>Tag Reads Failed</label>
                        <span className="stat-value">
                            {status.statistics?.tagReadsFailed?.toLocaleString() || '0'}
                        </span>
                    </div>

                    <div className="stat-item">
                        <label>Tag Read Success Rate</label>
                        <span className="stat-value">
                            {status.statistics?.tagReadSuccessRate?.toFixed(1) || '0'}%
                        </span>
                    </div>
                </div>
            </div>


        </div>
    );
};

export default StatusDashboard;

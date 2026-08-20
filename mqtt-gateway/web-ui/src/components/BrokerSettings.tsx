import React, { useState, useEffect } from 'react';
import { useSelector } from 'react-redux';
import { getBrokerConfig, saveBrokerConfig, deleteBroker, testConnection } from '../api';
import { MqttBrokerConfig, TestConnectionRequest } from '../types';

interface Props {
    onBrokersChanged: () => void;
}

const BrokerSettings: React.FC<Props> = ({ onBrokersChanged }) => {
    const csrfToken = useSelector((state: { userSession?: { csrfToken?: string } }) => state.userSession?.csrfToken || '');
    const [brokers, setBrokers] = useState<MqttBrokerConfig[]>([]);
    const [selectedBrokerId, setSelectedBrokerId] = useState<number | null>(null);
    const [editingBroker, setEditingBroker] = useState<MqttBrokerConfig | null>(null);
    const [isAddingNew, setIsAddingNew] = useState(false);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [testing, setTesting] = useState(false);
    const [certificateFileName, setCertificateFileName] = useState<string | null>(null);
    const [certificateInputKey, setCertificateInputKey] = useState(0);
    const [message, setMessage] = useState<{ type: 'success' | 'error' | 'info'; text: string } | null>(null);

    // Load all brokers on mount
    useEffect(() => {
        loadBrokers();
    }, []);

    const loadBrokers = async () => {
        setLoading(true);
        try {
            const response = await getBrokerConfig();
            if (response.success && response.data) {
                setBrokers(response.data);
                // Auto-select first broker if available
                if (response.data.length > 0 && !selectedBrokerId) {
                    selectBroker(response.data[0]);
                }
            }
        } catch (error) {
            console.error('Failed to load brokers:', error);
        } finally {
            setLoading(false);
        }
    };

    const selectBroker = (broker: MqttBrokerConfig) => {
        setSelectedBrokerId(broker.id || null);
        setEditingBroker({
            ...broker,
            tlsTrustMode: broker.tlsTrustMode || 'SYSTEM_DEFAULT',
            slowReconnectIntervalSeconds: broker.slowReconnectIntervalSeconds ?? 600
        });
        setCertificateFileName(null);
        setIsAddingNew(false);
        setMessage(null);
    };

    const handleAddNew = () => {
        const newBroker: MqttBrokerConfig = {
            name: 'New MQTT Broker',
            brokerUrl: 'tcp://localhost:1883',
            clientId: 'ignition-mqtt-publisher',
            username: '',
            password: '',
            useTls: false,
            tlsTrustMode: 'SYSTEM_DEFAULT',
            qos: 1,
            retained: false,
            cleanSession: true,
            connectionTimeout: 30,
            keepAliveInterval: 60,
            slowReconnectIntervalSeconds: 600,
            enabled: true
        };
        setEditingBroker(newBroker);
        setIsAddingNew(true);
        setSelectedBrokerId(null);
        setMessage(null);
        setCertificateFileName(null);
    };

    const normalizeUrlForTls = (url: string, enabled: boolean): string => {
        if (enabled) {
            return url
                .replace(/^mqtts:\/\//i, 'ssl://')
                .replace(/^mqtt:\/\//i, 'ssl://')
                .replace(/^tcp:\/\//i, 'ssl://');
        }
        return url.replace(/^ssl:\/\//i, 'tcp://').replace(/^mqtts:\/\//i, 'tcp://');
    };

    const handleTlsToggle = (e: React.ChangeEvent<HTMLInputElement>) => {
        const enabled = e.target.checked;
        setEditingBroker(prev => prev ? {
            ...prev,
            useTls: enabled,
            brokerUrl: normalizeUrlForTls(prev.brokerUrl, enabled)
        } : null);
    };

    const handleCertificateUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;
        if (file.size > 256 * 1024) {
            setMessage({ type: 'error', text: 'CA certificate bundle must be 256 KB or smaller' });
            setCertificateInputKey(key => key + 1);
            return;
        }

        try {
            const pem = await file.text();
            setEditingBroker(prev => prev ? {
                ...prev,
                caCertificatePem: pem,
                caCertificateConfigured: true,
                caCertificates: undefined,
                caCertificateError: undefined,
                removeCaCertificate: false,
                tlsTrustMode: 'UPLOADED_CA'
            } : null);
            setCertificateFileName(file.name);
            setMessage({ type: 'info', text: 'Certificate selected. Test or save the broker to validate it.' });
        } catch {
            setMessage({ type: 'error', text: 'Unable to read the selected certificate file' });
        }
    };

    const handleRemoveCertificate = () => {
        setEditingBroker(prev => prev ? {
            ...prev,
            caCertificatePem: undefined,
            caCertificateConfigured: false,
            caCertificates: [],
            caCertificateError: undefined,
            removeCaCertificate: true
        } : null);
        setCertificateFileName(null);
        setCertificateInputKey(key => key + 1);
        setMessage({ type: 'info', text: 'The stored CA certificate will be removed when you save.' });
    };

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
        if (!editingBroker) return;

        const { name, value, type } = e.target;

        if (type === 'checkbox') {
            setEditingBroker(prev => prev ? {
                ...prev,
                [name]: (e.target as HTMLInputElement).checked
            } : null);
        } else if (type === 'number') {
            setEditingBroker(prev => prev ? {
                ...prev,
                [name]: parseInt(value, 10)
            } : null);
        } else {
            setEditingBroker(prev => prev ? {
                ...prev,
                [name]: value
            } : null);
        }
    };

    const handleTestConnection = async () => {
        if (!editingBroker) return;

        setTesting(true);
        setMessage(null);

        const testRequest: TestConnectionRequest = {
            id: editingBroker.id,
            brokerUrl: editingBroker.brokerUrl,
            clientId: editingBroker.clientId,
            username: editingBroker.username,
            password: editingBroker.password,
            useTls: editingBroker.useTls,
            tlsTrustMode: editingBroker.tlsTrustMode,
            caCertificatePem: editingBroker.caCertificatePem,
            removeCaCertificate: editingBroker.removeCaCertificate,
            connectionTimeout: editingBroker.connectionTimeout,
            keepAliveInterval: editingBroker.keepAliveInterval,
            cleanSession: editingBroker.cleanSession
        };

        try {
            const response = await testConnection(testRequest, csrfToken);

            if (response.success && response.data?.connected) {
                setMessage({
                    type: 'success',
                    text: `Connection successful! (${response.data.connectionTimeMs}ms)`
                });
            } else {
                setMessage({
                    type: 'error',
                    text: response.data?.message || response.error || 'Connection test failed'
                });
            }
        } catch (error) {
            setMessage({
                type: 'error',
                text: error instanceof Error ? error.message : 'Connection test failed'
            });
        } finally {
            setTesting(false);
        }
    };

    const handleSave = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!editingBroker) return;

        setSaving(true);
        setMessage(null);

        try {
            const response = await saveBrokerConfig(editingBroker, csrfToken);

            if (response.success && response.data) {
                setMessage({ type: 'success', text: 'Broker saved successfully' });
                setCertificateFileName(null);
                await loadBrokers();
                selectBroker(response.data);
                onBrokersChanged();
            } else {
                throw new Error(response.error || 'Failed to save broker');
            }
        } catch (error) {
            setMessage({
                type: 'error',
                text: error instanceof Error ? error.message : 'Failed to save broker'
            });
        } finally {
            setSaving(false);
        }
    };

    const handleDelete = async (brokerId: number) => {
        if (!confirm('Are you sure you want to delete this broker? This action cannot be undone.')) {
            return;
        }

        try {
            const response = await deleteBroker(brokerId, csrfToken);

            if (response.success) {
                setMessage({ type: 'success', text: 'Broker deleted successfully' });
                await loadBrokers();
                setEditingBroker(null);
                setSelectedBrokerId(null);
                onBrokersChanged();
            } else {
                setMessage({
                    type: 'error',
                    text: response.error || 'Failed to delete broker'
                });
            }
        } catch (error) {
            setMessage({
                type: 'error',
                text: error instanceof Error ? error.message : 'Failed to delete broker'
            });
        }
    };

    const handleCancel = () => {
        if (selectedBrokerId) {
            const broker = brokers.find(b => b.id === selectedBrokerId);
            if (broker) {
                setEditingBroker({ ...broker });
                setCertificateFileName(null);
            }
        } else {
            setEditingBroker(null);
            setIsAddingNew(false);
        }
        setMessage(null);
    };

    if (loading) {
        return <div className="broker-settings">Loading brokers...</div>;
    }

    return (
        <div className="broker-settings multi-broker">
            <div className="broker-list-panel">
                <div className="panel-header">
                    <h2>MQTT Brokers</h2>
                    <button 
                        type="button" 
                        onClick={handleAddNew}
                        className="btn-primary btn-small"
                    >
                        + Add Broker
                    </button>
                </div>

                <div className="broker-list">
                    {brokers.length === 0 ? (
                        <div className="empty-state">
                            No brokers configured. Click "Add Broker" to get started.
                        </div>
                    ) : (
                        brokers.map(broker => (
                            <div
                                key={broker.id}
                                className={`broker-item ${selectedBrokerId === broker.id ? 'selected' : ''}`}
                                onClick={() => selectBroker(broker)}
                            >
                                <div className="broker-item-header">
                                    <strong>{broker.name}</strong>
                                    {broker.enabled && <span className="badge badge-success">Enabled</span>}
                                </div>
                                <div className="broker-item-url">{broker.brokerUrl}</div>
                            </div>
                        ))
                    )}
                </div>
            </div>

            <div className="broker-edit-panel">
                {editingBroker ? (
                    <form onSubmit={handleSave}>
                        <div className="panel-header">
                            <h2>{isAddingNew ? 'Add New Broker' : 'Edit Broker'}</h2>
                        </div>

                        <div className="form-section">
                            <div className="form-group">
                                <label htmlFor="name">Broker Name</label>
                                <input
                                    type="text"
                                    id="name"
                                    name="name"
                                    value={editingBroker.name}
                                    onChange={handleChange}
                                    placeholder="e.g., Production MQTT, Development Broker"
                                    required
                                />
                                <small>Friendly name to identify this broker</small>
                            </div>
                        </div>

                        <div className="form-section">
                            <h3>Connection Settings</h3>

                            <div className="form-group">
                                <label htmlFor="brokerUrl">Broker URL</label>
                                <input
                                    type="text"
                                    id="brokerUrl"
                                    name="brokerUrl"
                                    value={editingBroker.brokerUrl}
                                    onChange={handleChange}
                                    placeholder="tcp://localhost:1883"
                                    required
                                />
                                <small>Format: tcp://hostname:port or ssl://hostname:port</small>
                            </div>

                            <div className="form-group">
                                <label htmlFor="clientId">Client ID</label>
                                <input
                                    type="text"
                                    id="clientId"
                                    name="clientId"
                                    value={editingBroker.clientId}
                                    onChange={handleChange}
                                    placeholder="ignition-mqtt-publisher"
                                    required
                                />
                            </div>

                            <div className="form-row">
                                <div className="form-group">
                                    <label htmlFor="username">Username (optional)</label>
                                    <input
                                        type="text"
                                        id="username"
                                        name="username"
                                        value={editingBroker.username || ''}
                                        onChange={handleChange}
                                        placeholder="Leave empty for anonymous"
                                    />
                                </div>

                                <div className="form-group">
                                    <label htmlFor="password">Password (optional)</label>
                                    <input
                                        type="password"
                                        id="password"
                                        name="password"
                                        value={editingBroker.password || ''}
                                        onChange={handleChange}
                                        placeholder={editingBroker.hasPassword ? 'Stored password will be preserved' : 'Leave empty for no password'}
                                    />
                                </div>
                            </div>
                        </div>

                        <div className="form-section">
                            <h3>MQTT Settings</h3>

                            <div className="form-row">
                                <div className="form-group">
                                    <label htmlFor="qos">Quality of Service (QoS)</label>
                                    <select id="qos" name="qos" value={editingBroker.qos} onChange={handleChange}>
                                        <option value={0}>0 - At most once</option>
                                        <option value={1}>1 - At least once</option>
                                        <option value={2}>2 - Exactly once</option>
                                    </select>
                                </div>

                                <div className="form-group">
                                    <label htmlFor="connectionTimeout">Connection Timeout (seconds)</label>
                                    <input
                                        type="number"
                                        id="connectionTimeout"
                                        name="connectionTimeout"
                                        value={editingBroker.connectionTimeout}
                                        onChange={handleChange}
                                        min={5}
                                        max={300}
                                    />
                                </div>

                                <div className="form-group">
                                    <label htmlFor="keepAliveInterval">Keep Alive Interval (seconds)</label>
                                    <input
                                        type="number"
                                        id="keepAliveInterval"
                                        name="keepAliveInterval"
                                        value={editingBroker.keepAliveInterval}
                                        onChange={handleChange}
                                        min={10}
                                        max={3600}
                                    />
                                </div>
                            </div>

                            <div className="form-row">
                                <div className="form-group">
                                    <label htmlFor="slowReconnectIntervalSeconds">Slow Reconnect Interval (seconds)</label>
                                    <input
                                        type="number"
                                        id="slowReconnectIntervalSeconds"
                                        name="slowReconnectIntervalSeconds"
                                        value={editingBroker.slowReconnectIntervalSeconds}
                                        onChange={handleChange}
                                        min={30}
                                        max={86400}
                                    />
                                    <small>Used after max reconnect attempts (e.g., 600 = 10 minutes)</small>
                                </div>
                            </div>

                            <div className="form-row">
                                <div className="form-group checkbox">
                                    <label>
                                        <input
                                            type="checkbox"
                                            name="useTls"
                                            checked={editingBroker.useTls}
                                            onChange={handleTlsToggle}
                                        />
                                        Use TLS/SSL encryption
                                    </label>
                                </div>

                                <div className="form-group checkbox">
                                    <label>
                                        <input
                                            type="checkbox"
                                            name="retained"
                                            checked={editingBroker.retained}
                                            onChange={handleChange}
                                        />
                                        Retain messages on broker
                                    </label>
                                </div>

                                <div className="form-group checkbox">
                                    <label>
                                        <input
                                            type="checkbox"
                                            name="cleanSession"
                                            checked={editingBroker.cleanSession}
                                            onChange={handleChange}
                                        />
                                        Clean session on connect
                                    </label>
                                </div>

                                <div className="form-group checkbox">
                                    <label>
                                        <input
                                            type="checkbox"
                                            name="enabled"
                                            checked={editingBroker.enabled}
                                            onChange={handleChange}
                                        />
                                        Enabled
                                    </label>
                                </div>
                            </div>

                            {editingBroker.useTls && (
                                <div className="tls-settings">
                                    <h3>TLS Trust</h3>
                                    <div className="form-group">
                                        <label htmlFor="tlsTrustMode">Certificate trust source</label>
                                        <select
                                            id="tlsTrustMode"
                                            name="tlsTrustMode"
                                            value={editingBroker.tlsTrustMode}
                                            onChange={handleChange}
                                        >
                                            <option value="SYSTEM_DEFAULT">Gateway JVM system trust store</option>
                                            <option value="UPLOADED_CA">Uploaded CA certificate bundle</option>
                                        </select>
                                        <small>Hostname verification is always enabled for TLS connections.</small>
                                    </div>

                                    {editingBroker.tlsTrustMode === 'UPLOADED_CA' && (
                                        <div className="certificate-upload">
                                            <div className="form-group">
                                                <label htmlFor="caCertificate">CA certificate or PEM bundle</label>
                                                <input
                                                    key={certificateInputKey}
                                                    id="caCertificate"
                                                    type="file"
                                                    accept=".pem,.crt,.cer,application/x-x509-ca-cert,text/plain"
                                                    onChange={handleCertificateUpload}
                                                />
                                                <small>PEM-encoded X.509 CA certificates only; maximum 256 KB and 16 certificates.</small>
                                            </div>

                                            {(certificateFileName || editingBroker.caCertificateConfigured) && (
                                                <div className="certificate-status">
                                                    <div>
                                                        <strong>{certificateFileName || 'Stored CA certificate bundle'}</strong>
                                                        <span>{certificateFileName ? 'Selected for validation' : 'Configured'}</span>
                                                    </div>
                                                    <button type="button" className="btn-secondary btn-small" onClick={handleRemoveCertificate}>
                                                        Remove
                                                    </button>
                                                </div>
                                            )}

                                            {editingBroker.caCertificateError && (
                                                <div className="certificate-warning">{editingBroker.caCertificateError}</div>
                                            )}

                                            {editingBroker.caCertificates?.map((certificate, index) => (
                                                <div className={`certificate-details ${certificate.currentlyValid ? '' : 'invalid'}`} key={certificate.sha256Fingerprint}>
                                                    <div><strong>Certificate {index + 1}</strong></div>
                                                    <div><span>Subject</span><code>{certificate.subject}</code></div>
                                                    <div><span>Issuer</span><code>{certificate.issuer}</code></div>
                                                    <div><span>Valid until</span><code>{new Date(certificate.notAfter).toLocaleString()}</code></div>
                                                    <div><span>SHA-256</span><code>{certificate.sha256Fingerprint}</code></div>
                                                    {!certificate.currentlyValid && (
                                                        <div className="certificate-warning">This certificate is outside its validity period.</div>
                                                    )}
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>

                        {message && (
                            <div className={`message ${message.type}`}>
                                {message.text}
                            </div>
                        )}

                        <div className="form-actions">
                            <div className="left-actions">
                                {!isAddingNew && editingBroker.id && (
                                    <button
                                        type="button"
                                        onClick={() => handleDelete(editingBroker.id!)}
                                        className="btn-danger"
                                    >
                                        Delete Broker
                                    </button>
                                )}
                            </div>
                            
                            <div className="right-actions">
                                <button
                                    type="button"
                                    onClick={handleTestConnection}
                                    disabled={testing || saving}
                                    className="btn-secondary"
                                >
                                    {testing ? 'Testing...' : 'Test Connection'}
                                </button>

                                <button
                                    type="button"
                                    onClick={handleCancel}
                                    disabled={saving || testing}
                                    className="btn-secondary"
                                >
                                    Cancel
                                </button>

                                <button
                                    type="submit"
                                    disabled={saving || testing}
                                    className="btn-primary"
                                >
                                    {saving ? 'Saving...' : 'Save Broker'}
                                </button>
                            </div>
                        </div>
                    </form>
                ) : (
                    <div className="empty-state">
                        Select a broker from the list or add a new one.
                    </div>
                )}
            </div>
        </div>
    );
};

export default BrokerSettings;

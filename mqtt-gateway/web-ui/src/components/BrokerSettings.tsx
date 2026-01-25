import React, { useState, useEffect } from 'react';
import { saveBrokerConfig, testConnection } from '../api';
import { MqttBrokerConfig, TestConnectionRequest } from '../types';

interface Props {
    config: MqttBrokerConfig | null;
    onConfigSaved: (config: MqttBrokerConfig) => void;
}

const BrokerSettings: React.FC<Props> = ({ config, onConfigSaved }) => {
    const [formData, setFormData] = useState<MqttBrokerConfig>({
        brokerUrl: 'tcp://localhost:1883',
        clientId: 'ignition-mqtt-publisher',
        username: '',
        password: '',
        useTls: false,
        qos: 1,
        retained: false,
        cleanSession: true,
        connectionTimeout: 30,
        keepAliveInterval: 60,
        enabled: true  // Always enabled - control is now at mapping level
    });

    const [saving, setSaving] = useState(false);
    const [testing, setTesting] = useState(false);
    const [message, setMessage] = useState<{ type: 'success' | 'error' | 'info'; text: string } | null>(null);

    useEffect(() => {
        if (config) {
            setFormData(config);
        }
    }, [config]);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
        const { name, value, type } = e.target;

        if (type === 'checkbox') {
            setFormData(prev => ({
                ...prev,
                [name]: (e.target as HTMLInputElement).checked
            }));
        } else if (type === 'number') {
            setFormData(prev => ({
                ...prev,
                [name]: parseInt(value, 10)
            }));
        } else {
            setFormData(prev => ({
                ...prev,
                [name]: value
            }));
        }
    };

    const handleTestConnection = async () => {
        setTesting(true);
        setMessage(null);

        const testRequest: TestConnectionRequest = {
            brokerUrl: formData.brokerUrl,
            clientId: formData.clientId,
            username: formData.username,
            password: formData.password,
            useTls: formData.useTls,
            connectionTimeout: formData.connectionTimeout,
            keepAliveInterval: formData.keepAliveInterval,
            cleanSession: formData.cleanSession
        };

        try {
            const response = await testConnection(testRequest);

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
        setSaving(true);
        setMessage(null);

        try {
            const response = await saveBrokerConfig(formData);

            if (response.success && response.data) {
                setMessage({ type: 'success', text: 'Configuration saved successfully' });
                onConfigSaved(response.data);
            } else {
                throw new Error(response.error || 'Failed to save configuration');
            }
        } catch (error) {
            setMessage({
                type: 'error',
                text: error instanceof Error ? error.message : 'Failed to save configuration'
            });
        } finally {
            setSaving(false);
        }
    };

    return (
        <div className="broker-settings">
            <form onSubmit={handleSave}>
                <div className="form-section">
                    <h2>Connection Settings</h2>

                    <div className="form-group">
                        <label htmlFor="brokerUrl">Broker URL</label>
                        <input
                            type="text"
                            id="brokerUrl"
                            name="brokerUrl"
                            value={formData.brokerUrl}
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
                            value={formData.clientId}
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
                                value={formData.username || ''}
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
                                value={formData.password || ''}
                                onChange={handleChange}
                                placeholder="Leave empty for no password"
                            />
                        </div>
                    </div>
                </div>

                <div className="form-section">
                    <h2>MQTT Settings</h2>

                    <div className="form-row">
                        <div className="form-group">
                            <label htmlFor="qos">Quality of Service (QoS)</label>
                            <select id="qos" name="qos" value={formData.qos} onChange={handleChange}>
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
                                value={formData.connectionTimeout}
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
                                value={formData.keepAliveInterval}
                                onChange={handleChange}
                                min={10}
                                max={3600}
                            />
                        </div>
                    </div>

                    <div className="form-row">
                        <div className="form-group checkbox">
                            <label>
                                <input
                                    type="checkbox"
                                    name="useTls"
                                    checked={formData.useTls}
                                    onChange={handleChange}
                                />
                                Use TLS/SSL encryption
                            </label>
                        </div>

                        <div className="form-group checkbox">
                            <label>
                                <input
                                    type="checkbox"
                                    name="retained"
                                    checked={formData.retained}
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
                                    checked={formData.cleanSession}
                                    onChange={handleChange}
                                />
                                Clean session on connect
                            </label>
                        </div>
                    </div>
                </div>

                {message && (
                    <div className={`message ${message.type}`}>
                        {message.text}
                    </div>
                )}

                <div className="form-actions">
                    <button
                        type="button"
                        onClick={handleTestConnection}
                        disabled={testing || saving}
                        className="btn-secondary"
                    >
                        {testing ? 'Testing...' : 'Test Connection'}
                    </button>

                    <button
                        type="submit"
                        disabled={saving || testing}
                        className="btn-primary"
                    >
                        {saving ? 'Saving...' : 'Save Configuration'}
                    </button>
                </div>
            </form>
        </div>
    );
};

export default BrokerSettings;

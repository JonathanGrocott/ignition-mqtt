import React, { useEffect, useState } from 'react';
import { deleteBroker, getBrokerConfig, saveBrokerConfig } from '../api';
import { MqttBrokerConfig } from '../types';

const BrokerSettings: React.FC = () => {
    const [brokers, setBrokers] = useState<MqttBrokerConfig[]>([]);
    const [selectedBrokerId, setSelectedBrokerId] = useState<number | null>(null);
    const [editingBroker, setEditingBroker] = useState<MqttBrokerConfig | null>(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

    useEffect(() => {
        loadBrokers();
    }, []);

    const loadBrokers = async () => {
        setLoading(true);
        try {
            const response = await getBrokerConfig();
            if (response.success && response.data) {
                setBrokers(response.data);
                if (response.data.length > 0 && !selectedBrokerId) {
                    selectBroker(response.data[0]);
                }
            }
        } catch (error) {
            setMessage({ type: 'error', text: 'Failed to load brokers' });
        } finally {
            setLoading(false);
        }
    };

    const selectBroker = (broker: MqttBrokerConfig) => {
        setSelectedBrokerId(broker.id || null);
        setEditingBroker({ ...broker });
        setMessage(null);
    };

    const handleAddNew = () => {
        const newBroker: MqttBrokerConfig = {
            name: 'New MQTT Broker',
            brokerUrl: 'tcp://localhost:1883',
            clientId: 'ignition-sparkplug-publisher',
            username: '',
            password: '',
            useTls: false,
            qos: 1,
            retained: false,
            cleanSession: true,
            connectionTimeout: 30,
            keepAliveInterval: 60,
            enabled: true
        };
        setEditingBroker(newBroker);
        setSelectedBrokerId(null);
        setMessage(null);
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

    const handleSave = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!editingBroker) return;

        setSaving(true);
        setMessage(null);

        try {
            const response = await saveBrokerConfig(editingBroker);
            if (response.success && response.data) {
                setMessage({ type: 'success', text: 'Broker saved successfully' });
                await loadBrokers();
                selectBroker(response.data);
            } else {
                throw new Error(response.error || 'Failed to save broker');
            }
        } catch (error) {
            setMessage({ type: 'error', text: error instanceof Error ? error.message : 'Failed to save broker' });
        } finally {
            setSaving(false);
        }
    };

    const handleDelete = async (brokerId: number) => {
        if (!confirm('Are you sure you want to delete this broker?')) {
            return;
        }

        const response = await deleteBroker(brokerId);
        if (response.success) {
            setMessage({ type: 'success', text: 'Broker deleted successfully' });
            await loadBrokers();
            setEditingBroker(null);
            setSelectedBrokerId(null);
        } else {
            setMessage({ type: 'error', text: response.error || 'Failed to delete broker' });
        }
    };

    if (loading) {
        return <div>Loading brokers...</div>;
    }

    return (
        <div>
            {message && <div className={`message ${message.type}`}>{message.text}</div>}
            <div className="form-group">
                <label>Brokers</label>
                <div className="form-row">
                    <select
                        value={selectedBrokerId ?? ''}
                        onChange={(e) => {
                            const broker = brokers.find(b => b.id === Number(e.target.value));
                            if (broker) {
                                selectBroker(broker);
                            }
                        }}
                    >
                        <option value="">Select a broker</option>
                        {brokers.map(broker => (
                            <option key={broker.id} value={broker.id}>{broker.name}</option>
                        ))}
                    </select>
                    <button type="button" className="btn-secondary" onClick={handleAddNew}>New</button>
                </div>
            </div>

            {editingBroker && (
                <form onSubmit={handleSave}>
                    <div className="form-row">
                        <div className="form-group">
                            <label>Name</label>
                            <input name="name" value={editingBroker.name} onChange={handleChange} />
                        </div>
                        <div className="form-group">
                            <label>Broker URL</label>
                            <input name="brokerUrl" value={editingBroker.brokerUrl} onChange={handleChange} />
                        </div>
                        <div className="form-group">
                            <label>Client ID</label>
                            <input name="clientId" value={editingBroker.clientId} onChange={handleChange} />
                        </div>
                    </div>
                    <div className="form-row">
                        <div className="form-group">
                            <label>Username</label>
                            <input name="username" value={editingBroker.username || ''} onChange={handleChange} />
                        </div>
                        <div className="form-group">
                            <label>Password</label>
                            <input name="password" value={editingBroker.password || ''} onChange={handleChange} />
                        </div>
                    </div>
                    <div className="form-row">
                        <div className="form-group">
                            <label>QoS</label>
                            <input type="number" name="qos" value={editingBroker.qos} onChange={handleChange} />
                        </div>
                        <div className="form-group">
                            <label>Keep Alive (s)</label>
                            <input type="number" name="keepAliveInterval" value={editingBroker.keepAliveInterval} onChange={handleChange} />
                        </div>
                        <div className="form-group">
                            <label>Connection Timeout (s)</label>
                            <input type="number" name="connectionTimeout" value={editingBroker.connectionTimeout} onChange={handleChange} />
                        </div>
                    </div>
                    <div className="form-row">
                        <label>
                            <input type="checkbox" name="enabled" checked={editingBroker.enabled} onChange={handleChange} />
                            Enabled
                        </label>
                        <label>
                            <input type="checkbox" name="retained" checked={editingBroker.retained} onChange={handleChange} />
                            Retained
                        </label>
                        <label>
                            <input type="checkbox" name="cleanSession" checked={editingBroker.cleanSession} onChange={handleChange} />
                            Clean Session
                        </label>
                    </div>
                    <div className="mapping-actions">
                        <button type="submit" className="btn-primary" disabled={saving}>
                            {saving ? 'Saving...' : 'Save Broker'}
                        </button>
                        {editingBroker.id && (
                            <button type="button" className="btn-danger" onClick={() => handleDelete(editingBroker.id!)}>
                                Delete
                            </button>
                        )}
                    </div>
                </form>
            )}
        </div>
    );
};

export default BrokerSettings;

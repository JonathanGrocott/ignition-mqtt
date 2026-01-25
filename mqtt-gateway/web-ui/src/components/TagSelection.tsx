import React, { useState, useEffect } from 'react';
import { saveTagConfig, getBrokerConfig } from '../api';
import { MqttTagConfig, TopicMapping, MqttBrokerConfig } from '../types';

interface Props {
    config: MqttTagConfig | null;
    onConfigSaved: (config: MqttTagConfig) => void;
}

const TagSelection: React.FC<Props> = ({ config, onConfigSaved }) => {
    const [formData, setFormData] = useState<MqttTagConfig>({
        name: 'Default Tag Publishing',
        enabled: false,
        tagProviders: ['default'],
        tagFolders: [],
        topicMappings: [],
        topicOverrides: {},
        payloadTemplate: '',
        includeMetadata: true,
        valueDeadband: 0.1,
        publishOnQualityChange: true
    });

    const [brokers, setBrokers] = useState<MqttBrokerConfig[]>([]);
    const [selectedBrokerId, setSelectedBrokerId] = useState<number | null>(null);
    const [saving, setSaving] = useState(false);
    const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
    const [newMappingSource, setNewMappingSource] = useState('');
    const [newMappingTopic, setNewMappingTopic] = useState('');
    const [newMappingBrokerId, setNewMappingBrokerId] = useState<number | null>(null);

    useEffect(() => {
        if (config) {
            setFormData(config);
        }
        // Always reload brokers when component mounts or config changes
        loadBrokers();
    }, [config]);
    
    // Also reload brokers when tab becomes visible (e.g., after adding a broker)
    useEffect(() => {
        const handleFocus = () => {
            loadBrokers();
        };
        window.addEventListener('focus', handleFocus);
        return () => window.removeEventListener('focus', handleFocus);
    }, []);

    const loadBrokers = async () => {
        try {
            const response = await getBrokerConfig();
            if (response.success && response.data) {
                setBrokers(response.data);
                // Auto-select first broker for new mappings
                if (response.data.length > 0 && !newMappingBrokerId) {
                    setNewMappingBrokerId(response.data[0].id || null);
                }
            }
        } catch (error) {
            console.error('Failed to load brokers:', error);
        }
    };

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
        const { name, value, type } = e.target;

        if (type === 'checkbox') {
            setFormData(prev => ({
                ...prev,
                [name]: (e.target as HTMLInputElement).checked
            }));
        } else if (type === 'number') {
            setFormData(prev => ({
                ...prev,
                [name]: parseFloat(value)
            }));
        } else {
            setFormData(prev => ({
                ...prev,
                [name]: value
            }));
        }
    };

    const addTopicMapping = () => {
        if (newMappingSource && newMappingTopic && newMappingBrokerId) {
            const newMapping: TopicMapping = {
                id: Date.now().toString(),
                brokerId: newMappingBrokerId,
                sourcePattern: newMappingSource,
                topicPrefix: newMappingTopic,
                enabled: true
            };
            setFormData(prev => ({
                ...prev,
                topicMappings: [...prev.topicMappings, newMapping]
            }));
            setNewMappingSource('');
            setNewMappingTopic('');
            // Keep the same broker selected for convenience
        } else {
            setMessage({
                type: 'error',
                text: 'Please fill in all fields and select a broker'
            });
        }
    };

    const removeTopicMapping = (id: string) => {
        setFormData(prev => ({
            ...prev,
            topicMappings: prev.topicMappings.filter(m => m.id !== id)
        }));
    };

    const toggleMappingEnabled = (id: string) => {
        setFormData(prev => ({
            ...prev,
            topicMappings: prev.topicMappings.map(m =>
                m.id === id ? { ...m, enabled: !m.enabled } : m
            )
        }));
    };

    const handleSave = async (e: React.FormEvent) => {
        e.preventDefault();
        setSaving(true);
        setMessage(null);

        try {
            const response = await saveTagConfig(formData);

            if (response.success && response.data) {
                setMessage({ type: 'success', text: 'Tag configuration saved successfully' });
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

    // Group mappings by broker
    const mappingsByBroker = formData.topicMappings.reduce((acc, mapping) => {
        const key = mapping.brokerId ?? 'unassigned';
        if (!acc[key]) {
            acc[key] = [];
        }
        acc[key].push(mapping);
        return acc;
    }, {} as Record<number | 'unassigned', TopicMapping[]>);

    const getBrokerName = (brokerId: number | 'unassigned') => {
        if (brokerId === 'unassigned') {
            return 'Unassigned (No Broker)';
        }
        const broker = brokers.find(b => b.id === brokerId);
        return broker ? broker.name : `Broker ${brokerId}`;
    };

    return (
        <div className="tag-selection">
            <form onSubmit={handleSave}>
                <div className="form-section">
                    <h2>UNS Topic Mappings</h2>
                    <p className="section-description">
                        Map tag providers or folders to custom UNS topic prefixes and assign them to MQTT brokers. 
                        Only tags matching enabled mappings will be published to their assigned broker.
                        Example: Map <code>[Sample_Tags]Random</code> to <code>enterprise/site1/line1</code>
                    </p>

                    {brokers.length === 0 ? (
                        <div className="warning-message">
                            <strong>No brokers configured.</strong> Please configure at least one MQTT broker in the Broker Settings tab before creating topic mappings.
                            <button type="button" onClick={loadBrokers} className="btn-secondary" style={{marginLeft: '10px'}}>
                                Refresh
                            </button>
                        </div>
                    ) : (
                        <>
                            <div className="form-group">
                                <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px'}}>
                                    <label>Add New Topic Mapping</label>
                                    <button type="button" onClick={loadBrokers} className="btn-secondary" style={{fontSize: '12px', padding: '4px 10px'}}>
                                        🔄 Refresh Brokers
                                    </button>
                                </div>
                                <div className="mapping-input-container">
                                    <div className="mapping-input-row">
                                        <select
                                            value={newMappingBrokerId || ''}
                                            onChange={(e) => setNewMappingBrokerId(Number(e.target.value))}
                                            className="broker-select"
                                        >
                                            <option value="">Select broker...</option>
                                            {brokers.map(broker => (
                                                <option key={broker.id} value={broker.id}>
                                                    {broker.name} ({broker.brokerUrl})
                                                </option>
                                            ))}
                                        </select>
                                    </div>
                                    <div className="mapping-input-row">
                                        <input
                                            type="text"
                                            value={newMappingSource}
                                            onChange={(e) => setNewMappingSource(e.target.value)}
                                            placeholder="Source pattern (e.g., [default]TestTags)"
                                            className="mapping-source"
                                        />
                                        <span className="mapping-arrow">→</span>
                                        <input
                                            type="text"
                                            value={newMappingTopic}
                                            onChange={(e) => setNewMappingTopic(e.target.value)}
                                            placeholder="UNS topic prefix (e.g., enterprise/site1/area2)"
                                            className="mapping-topic"
                                        />
                                        <button type="button" onClick={addTopicMapping} className="btn-add">
                                            Add Mapping
                                        </button>
                                    </div>
                                </div>
                            </div>

                            {formData.topicMappings.length > 0 ? (
                                <div className="mappings-by-broker">
                                    {selectedBrokerId === null ? (
                                        // Show all brokers (and unassigned)
                                        <>
                                            {/* Show unassigned mappings first with a warning */}
                                            {mappingsByBroker['unassigned'] && mappingsByBroker['unassigned'].length > 0 && (
                                                <div key="unassigned" className="broker-mappings-group unassigned-group">
                                                    <h3 className="broker-group-header">
                                                        ⚠️ Unassigned Mappings (No Broker)
                                                        <span className="mapping-count">{mappingsByBroker['unassigned'].length} mapping{mappingsByBroker['unassigned'].length !== 1 ? 's' : ''}</span>
                                                    </h3>
                                                    <p className="warning-text">These mappings were created before multi-broker support. Delete them and recreate with a broker assigned.</p>
                                                    <div className="mappings-list">
                                                        {mappingsByBroker['unassigned'].map(mapping => (
                                                            <div key={mapping.id} className="mapping-item disabled">
                                                                <div className="mapping-details">
                                                                    <span className="mapping-source-display">{mapping.sourcePattern}</span>
                                                                    <span className="mapping-arrow">→</span>
                                                                    <span className="mapping-topic-display">{mapping.topicPrefix}</span>
                                                                </div>
                                                                <div className="mapping-actions">
                                                                    <button
                                                                        type="button"
                                                                        onClick={() => removeTopicMapping(mapping.id!)}
                                                                        className="btn-remove-small"
                                                                        title="Delete this unassigned mapping"
                                                                    >
                                                                        ✕ Delete
                                                                    </button>
                                                                </div>
                                                            </div>
                                                        ))}
                                                    </div>
                                                </div>
                                            )}
                                            
                                            {/* Show assigned brokers */}
                                            {brokers.map(broker => {
                                                const brokerMappings = mappingsByBroker[broker.id!] || [];
                                                if (brokerMappings.length === 0) return null;
                                                
                                                return (
                                                    <div key={broker.id} className="broker-mappings-group">
                                                        <h3 className="broker-group-header">
                                                            {broker.name}
                                                            <span className="broker-url">{broker.brokerUrl}</span>
                                                            <span className="mapping-count">{brokerMappings.length} mapping{brokerMappings.length !== 1 ? 's' : ''}</span>
                                                        </h3>
                                                        <div className="mappings-list">
                                                            {brokerMappings.map(mapping => (
                                                                <div key={mapping.id} className={`mapping-item ${!mapping.enabled ? 'disabled' : ''}`}>
                                                                    <div className="mapping-details">
                                                                        <span className="mapping-source-display">{mapping.sourcePattern}</span>
                                                                        <span className="mapping-arrow">→</span>
                                                                        <span className="mapping-topic-display">{mapping.topicPrefix}</span>
                                                                    </div>
                                                                    <div className="mapping-actions">
                                                                        <label className="toggle-switch">
                                                                            <input
                                                                                type="checkbox"
                                                                                checked={mapping.enabled}
                                                                                onChange={() => toggleMappingEnabled(mapping.id!)}
                                                                            />
                                                                            <span className="toggle-slider"></span>
                                                                        </label>
                                                                        <button
                                                                            type="button"
                                                                            onClick={() => removeTopicMapping(mapping.id!)}
                                                                            className="btn-remove-small"
                                                                        >
                                                                            ✕
                                                                        </button>
                                                                    </div>
                                                                </div>
                                                            ))}
                                                        </div>
                                                    </div>
                                                );
                                            })}
                                        </>
                                    ) : (
                                        // Show selected broker only
                                        <div className="broker-mappings-group">
                                            <h3>{getBrokerName(selectedBrokerId)}</h3>
                                            <div className="mappings-list">
                                                {(mappingsByBroker[selectedBrokerId] || []).map(mapping => (
                                                    <div key={mapping.id} className={`mapping-item ${!mapping.enabled ? 'disabled' : ''}`}>
                                                        <div className="mapping-details">
                                                            <span className="mapping-source-display">{mapping.sourcePattern}</span>
                                                            <span className="mapping-arrow">→</span>
                                                            <span className="mapping-topic-display">{mapping.topicPrefix}</span>
                                                        </div>
                                                        <div className="mapping-actions">
                                                            <label className="toggle-switch">
                                                                <input
                                                                    type="checkbox"
                                                                    checked={mapping.enabled}
                                                                    onChange={() => toggleMappingEnabled(mapping.id!)}
                                                                />
                                                                <span className="toggle-slider"></span>
                                                            </label>
                                                            <button
                                                                type="button"
                                                                onClick={() => removeTopicMapping(mapping.id!)}
                                                                className="btn-remove-small"
                                                            >
                                                                ✕
                                                            </button>
                                                        </div>
                                                    </div>
                                                ))}
                                            </div>
                                        </div>
                                    )}
                                </div>
                            ) : (
                                <p className="no-mappings">
                                    No custom topic mappings configured yet.
                                </p>
                            )}
                            
                            <small>Custom mappings override default topic generation for matching tags</small>
                        </>
                    )}
                </div>

                <div className="form-section">
                    <h2>Publishing Settings</h2>

                    <div className="form-group">
                        <label htmlFor="valueDeadband">Value Deadband</label>
                        <input
                            type="number"
                            id="valueDeadband"
                            name="valueDeadband"
                            value={formData.valueDeadband}
                            onChange={handleChange}
                            min={0}
                            step={0.01}
                        />
                        <small>Minimum change required to publish (prevents noise from small fluctuations)</small>
                    </div>

                    <div className="form-group">
                        <label htmlFor="payloadTemplate">Payload Template (optional)</label>
                        <textarea
                            id="payloadTemplate"
                            name="payloadTemplate"
                            value={formData.payloadTemplate || ''}
                            onChange={handleChange}
                            placeholder="Leave empty for default JSON format"
                            rows={4}
                        />
                        <small>Custom JSON template for message payload (advanced users only)</small>
                    </div>

                    <div className="form-group checkbox">
                        <label>
                            <input
                                type="checkbox"
                                name="includeMetadata"
                                checked={formData.includeMetadata}
                                onChange={handleChange}
                            />
                            Include metadata (timestamp, quality, datatype)
                        </label>
                    </div>

                    <div className="form-group checkbox">
                        <label>
                            <input
                                type="checkbox"
                                name="publishOnQualityChange"
                                checked={formData.publishOnQualityChange}
                                onChange={handleChange}
                            />
                            Publish when tag quality changes
                        </label>
                    </div>
                </div>

                <div className="form-section">
                    <h2>Module Settings</h2>

                    <div className="form-group checkbox">
                        <label>
                            <input
                                type="checkbox"
                                name="enabled"
                                checked={formData.enabled}
                                onChange={handleChange}
                            />
                            <strong>Enable tag publishing</strong>
                        </label>
                        <small>Tag publishing must be enabled to monitor and publish tag changes</small>
                    </div>
                </div>

                {message && (
                    <div className={`message ${message.type}`}>
                        {message.text}
                    </div>
                )}

                <div className="form-actions">
                    <button
                        type="submit"
                        disabled={saving}
                        className="btn-primary"
                    >
                        {saving ? 'Saving...' : 'Save Configuration'}
                    </button>
                </div>
            </form>
        </div>
    );
};

export default TagSelection;

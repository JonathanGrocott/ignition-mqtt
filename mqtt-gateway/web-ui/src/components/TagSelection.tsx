import React, { useState, useEffect } from 'react';
import { saveTagConfig } from '../api';
import { MqttTagConfig, TopicMapping } from '../types';

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

    const [saving, setSaving] = useState(false);
    const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
    const [newMappingSource, setNewMappingSource] = useState('');
    const [newMappingTopic, setNewMappingTopic] = useState('');

    useEffect(() => {
        if (config) {
            setFormData(config);
        }
    }, [config]);

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
        if (newMappingSource && newMappingTopic) {
            const newMapping: TopicMapping = {
                id: Date.now().toString(),
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

    return (
        <div className="tag-selection">
            <form onSubmit={handleSave}>
                <div className="form-section">
                    <h2>UNS Topic Mappings</h2>
                    <p className="section-description">
                        Map tag providers or folders to custom UNS topic prefixes. 
                        Only tags matching enabled mappings will be published to MQTT.
                        Example: Map <code>[Sample_Tags]Random</code> to <code>enterprise/site1/line1</code>
                    </p>

                    <div className="form-group">
                        <label>Topic Mappings</label>
                        <div className="mapping-input">
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
                        
                        {formData.topicMappings.length > 0 ? (
                            <div className="mappings-list">
                                {formData.topicMappings.map(mapping => (
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
                        ) : (
                            <p className="no-mappings">
                                No custom topic mappings. Tags will use default mapping: <code>[provider]/folder/tag</code>
                            </p>
                        )}
                        
                        <small>Custom mappings override default topic generation for matching tags</small>
                    </div>
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

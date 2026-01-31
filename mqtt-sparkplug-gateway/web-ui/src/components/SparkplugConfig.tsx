import React, { useEffect, useState } from 'react';
import { getBrokerConfig, savePublishConfig } from '../api';
import { MqttBrokerConfig, SparkplugDeviceMapping, SparkplugPublishConfig } from '../types';

interface Props {
    config: SparkplugPublishConfig | null;
    onConfigSaved: (config: SparkplugPublishConfig) => void;
}

const SparkplugConfig: React.FC<Props> = ({ config, onConfigSaved }) => {
    const [brokers, setBrokers] = useState<MqttBrokerConfig[]>([]);
    const [formData, setFormData] = useState<SparkplugPublishConfig>(() => ({
        name: 'Default Sparkplug Publish',
        enabled: true,
        brokerId: 0,
        groupId: '',
        edgeNodeId: '',
        deviceMappings: []
    }));
    const [saving, setSaving] = useState(false);
    const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
    const [validationErrors, setValidationErrors] = useState<string[]>([]);
    const [newMapping, setNewMapping] = useState<SparkplugDeviceMapping>(() => ({
        sourcePattern: '',
        groupId: '',
        edgeNodeId: '',
        deviceId: '',
        enabled: true
    }));

    useEffect(() => {
        loadBrokers();
    }, []);

    useEffect(() => {
        if (config) {
            setFormData({
                ...config,
                deviceMappings: config.deviceMappings || []
            });
        }
    }, [config]);

    const loadBrokers = async () => {
        const response = await getBrokerConfig();
        if (response.success && response.data) {
            const brokerList = response.data;
            setBrokers(brokerList);
            if (!formData.brokerId && brokerList.length > 0) {
                setFormData(prev => ({ ...prev, brokerId: brokerList[0].id || 0 }));
            }
        }
    };

    const updateField = (key: keyof SparkplugPublishConfig, value: string | number | boolean) => {
        setFormData(prev => ({
            ...prev,
            [key]: value
        }));
        if (validationErrors.length > 0) {
            setValidationErrors([]);
        }
    };

    const updateMapping = (index: number, key: keyof SparkplugDeviceMapping, value: string | boolean) => {
        setFormData(prev => ({
            ...prev,
            deviceMappings: prev.deviceMappings.map((mapping, idx) => (
                idx === index ? { ...mapping, [key]: value } : mapping
            ))
        }));
        if (validationErrors.length > 0) {
            setValidationErrors([]);
        }
    };

    const addMapping = () => {
        const errors: string[] = [];
        const defaultGroupBlank = !formData.groupId || !formData.groupId.trim();
        const defaultEdgeBlank = !formData.edgeNodeId || !formData.edgeNodeId.trim();

        if (!newMapping.sourcePattern || !newMapping.sourcePattern.trim()) {
            errors.push('Tag folder is required for a new mapping.');
        }
        if (!newMapping.deviceId || !newMapping.deviceId.trim()) {
            errors.push('Device ID is required for a new mapping.');
        }
        if (defaultGroupBlank && (!newMapping.groupId || !newMapping.groupId.trim())) {
            errors.push('Group ID is required when defaults are blank.');
        }
        if (defaultEdgeBlank && (!newMapping.edgeNodeId || !newMapping.edgeNodeId.trim())) {
            errors.push('Edge Node ID is required when defaults are blank.');
        }

        if (errors.length > 0) {
            setValidationErrors(errors);
            setMessage({ type: 'error', text: 'Fix validation errors before adding a mapping.' });
            return;
        }

        const mappingToAdd: SparkplugDeviceMapping = {
            sourcePattern: newMapping.sourcePattern.trim(),
            groupId: newMapping.groupId?.trim() || formData.groupId || '',
            edgeNodeId: newMapping.edgeNodeId?.trim() || formData.edgeNodeId || '',
            deviceId: newMapping.deviceId.trim(),
            enabled: newMapping.enabled
        };
        setFormData(prev => ({
            ...prev,
            deviceMappings: [...prev.deviceMappings, mappingToAdd]
        }));
        setNewMapping({
            sourcePattern: '',
            groupId: '',
            edgeNodeId: '',
            deviceId: '',
            enabled: true
        });
        setValidationErrors([]);
        setMessage(null);
    };

    const removeMapping = (index: number) => {
        setFormData(prev => ({
            ...prev,
            deviceMappings: prev.deviceMappings.filter((_, idx) => idx !== index)
        }));
    };

    const setAllMappingsEnabled = (enabled: boolean) => {
        setFormData(prev => ({
            ...prev,
            deviceMappings: prev.deviceMappings.map(mapping => ({
                ...mapping,
                enabled
            }))
        }));
    };

    const validateConfig = () => {
        const errors: string[] = [];
        if (!formData.name || !formData.name.trim()) {
            errors.push('Configuration name is required.');
        }
        if (!formData.brokerId) {
            errors.push('Select a broker for this configuration.');
        }

        const defaultGroup = formData.groupId?.trim() ?? '';
        const defaultEdge = formData.edgeNodeId?.trim() ?? '';
        const requiresMappingGroup = defaultGroup.length === 0;
        const requiresMappingEdge = defaultEdge.length === 0;

        formData.deviceMappings.forEach((mapping, index) => {
            const prefix = `Mapping ${index + 1}`;
            if (!mapping.sourcePattern || !mapping.sourcePattern.trim()) {
                errors.push(`${prefix}: Tag folder is required.`);
            }
            if (!mapping.deviceId || !mapping.deviceId.trim()) {
                errors.push(`${prefix}: Device ID is required.`);
            }
            if (requiresMappingGroup && (!mapping.groupId || !mapping.groupId.trim())) {
                errors.push(`${prefix}: Group ID is required when defaults are blank.`);
            }
            if (requiresMappingEdge && (!mapping.edgeNodeId || !mapping.edgeNodeId.trim())) {
                errors.push(`${prefix}: Edge Node ID is required when defaults are blank.`);
            }
        });

        return errors;
    };

    const handleSave = async (e: React.FormEvent) => {
        e.preventDefault();
        const errors = validateConfig();
        if (errors.length > 0) {
            setValidationErrors(errors);
            setMessage({ type: 'error', text: 'Fix validation errors before saving.' });
            return;
        }
        setValidationErrors([]);
        setSaving(true);
        setMessage(null);

        try {
            const response = await savePublishConfig(formData);
            if (response.success && response.data) {
                setMessage({ type: 'success', text: 'Sparkplug configuration saved.' });
                onConfigSaved(response.data);
            } else {
                throw new Error(response.error || 'Failed to save config');
            }
        } catch (error) {
            setMessage({ type: 'error', text: error instanceof Error ? error.message : 'Failed to save config' });
        } finally {
            setSaving(false);
        }
    };

    const defaultGroupBlank = !formData.groupId || !formData.groupId.trim();
    const defaultEdgeBlank = !formData.edgeNodeId || !formData.edgeNodeId.trim();

    return (
        <form onSubmit={handleSave}>
            {message && <div className={`message ${message.type}`}>{message.text}</div>}

            {validationErrors.length > 0 && (
                <div className="validation-errors">
                    <strong>Missing required fields:</strong>
                    <ul>
                        {validationErrors.map((error, index) => (
                            <li key={`${error}-${index}`}>{error}</li>
                        ))}
                    </ul>
                </div>
            )}

            <div className="form-row">
                <div className="form-group">
                    <label>Name</label>
                    <input value={formData.name} onChange={(e) => updateField('name', e.target.value)} />
                </div>
                <div className="form-group">
                    <label>Broker</label>
                    <select
                        value={formData.brokerId}
                        onChange={(e) => updateField('brokerId', Number(e.target.value))}
                    >
                        {brokers.map(broker => (
                            <option key={broker.id} value={broker.id}>{broker.name}</option>
                        ))}
                    </select>
                </div>
                <div className="form-group">
                    <label>
                        <input
                            type="checkbox"
                            checked={formData.enabled}
                            onChange={(e) => updateField('enabled', e.target.checked)}
                        />
                        Enabled
                    </label>
                </div>
            </div>

            <div className="form-row">
                <div className="form-group">
                    <label>Default Group ID</label>
                    <input value={formData.groupId || ''} onChange={(e) => updateField('groupId', e.target.value)} />
                </div>
                <div className="form-group">
                    <label>Default Edge Node ID</label>
                    <input value={formData.edgeNodeId || ''} onChange={(e) => updateField('edgeNodeId', e.target.value)} />
                </div>
            </div>

            <div className="form-group">
                <label>Device Mappings</label>
                <div className="mapping-toolbar">
                    <span>{formData.deviceMappings.length} mapping{formData.deviceMappings.length === 1 ? '' : 's'}</span>
                    <div className="mapping-toolbar-actions">
                        <button type="button" className="btn-secondary" onClick={() => setAllMappingsEnabled(true)}>Enable all</button>
                        <button type="button" className="btn-secondary" onClick={() => setAllMappingsEnabled(false)}>Disable all</button>
                    </div>
                </div>
                <div className="mapping-add">
                    <div className="mapping-add-row">
                        <div className="form-group">
                            <label>Tag Folder</label>
                            <input
                                value={newMapping.sourcePattern}
                                placeholder="[default]Folder"
                                onChange={(e) => setNewMapping(prev => ({ ...prev, sourcePattern: e.target.value }))}
                            />
                        </div>
                        <div className="form-group">
                            <label>Group ID</label>
                            <input
                                value={newMapping.groupId}
                                placeholder={formData.groupId || 'Default from config'}
                                onChange={(e) => setNewMapping(prev => ({ ...prev, groupId: e.target.value }))}
                            />
                        </div>
                        <div className="form-group">
                            <label>Edge Node ID</label>
                            <input
                                value={newMapping.edgeNodeId}
                                placeholder={formData.edgeNodeId || 'Default from config'}
                                onChange={(e) => setNewMapping(prev => ({ ...prev, edgeNodeId: e.target.value }))}
                            />
                        </div>
                        <div className="form-group">
                            <label>Device ID</label>
                            <input
                                value={newMapping.deviceId}
                                placeholder="Device"
                                onChange={(e) => setNewMapping(prev => ({ ...prev, deviceId: e.target.value }))}
                            />
                        </div>
                        <div className="form-group mapping-add-actions">
                            <label>
                                <input
                                    type="checkbox"
                                    checked={newMapping.enabled}
                                    onChange={(e) => setNewMapping(prev => ({ ...prev, enabled: e.target.checked }))}
                                />
                                Enabled
                            </label>
                            <button type="button" className="btn-secondary" onClick={addMapping}>+ Add Mapping</button>
                        </div>
                    </div>
                </div>
                <div className="mapping-list">
                    {formData.deviceMappings.map((mapping, index) => (
                        <div key={index} className={`mapping-item ${mapping.enabled ? '' : 'disabled'}`}>
                            <div className="mapping-summary">
                                <div className="mapping-summary-text">
                                    <span className="mapping-summary-label">{mapping.sourcePattern || 'New mapping'}</span>
                                    <span className="mapping-summary-arrow">→</span>
                                    <span className="mapping-summary-label">{mapping.deviceId || 'Device'}</span>
                                </div>
                                <div className="mapping-summary-actions">
                                    <label className="mapping-toggle">
                                        <input
                                            type="checkbox"
                                            checked={mapping.enabled}
                                            onChange={(e) => updateMapping(index, 'enabled', e.target.checked)}
                                        />
                                    </label>
                                    <button type="button" className="btn-danger" onClick={() => removeMapping(index)}>
                                        Delete
                                    </button>
                                </div>
                            </div>
                            <div className="form-row">
                                <div className="form-group">
                                    <label>Tag Folder</label>
                                    <input
                                        value={mapping.sourcePattern}
                                        onChange={(e) => updateMapping(index, 'sourcePattern', e.target.value)}
                                    />
                                </div>
                                <div className="form-group">
                                    <label>Group ID</label>
                                    <input
                                        value={mapping.groupId || ''}
                                        onChange={(e) => updateMapping(index, 'groupId', e.target.value)}
                                    />
                                    {defaultGroupBlank && (!mapping.groupId || !mapping.groupId.trim()) && (
                                        <small className="field-warning">Required when defaults are blank</small>
                                    )}
                                </div>
                                <div className="form-group">
                                    <label>Edge Node ID</label>
                                    <input
                                        value={mapping.edgeNodeId || ''}
                                        onChange={(e) => updateMapping(index, 'edgeNodeId', e.target.value)}
                                    />
                                    {defaultEdgeBlank && (!mapping.edgeNodeId || !mapping.edgeNodeId.trim()) && (
                                        <small className="field-warning">Required when defaults are blank</small>
                                    )}
                                </div>
                                <div className="form-group">
                                    <label>Device ID</label>
                                    <input
                                        value={mapping.deviceId}
                                        onChange={(e) => updateMapping(index, 'deviceId', e.target.value)}
                                    />
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
                <button type="button" className="btn-secondary" onClick={addMapping}>+ Add Mapping</button>
            </div>

            <button type="submit" className="btn-primary" disabled={saving}>
                {saving ? 'Saving...' : 'Save Sparkplug Config'}
            </button>
        </form>
    );
};

export default SparkplugConfig;

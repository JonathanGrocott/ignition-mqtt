import React, { useState, useEffect } from 'react';
import { getBrokerConfig, getTagConfig } from '../api';
import { MqttBrokerConfig, MqttTagConfig } from '../types';
import BrokerSettings from './BrokerSettings';
import TagSelection from './TagSelection';
import StatusDashboard from './StatusDashboard';
import '../styles.css';

type TabType = 'broker' | 'tags' | 'status';

const Configuration: React.FC = () => {
    console.log('[Configuration] Component rendering started');
    
    const [activeTab, setActiveTab] = useState<TabType>('broker');
    const [brokerConfig, setBrokerConfig] = useState<MqttBrokerConfig | null>(null);
    const [tagConfig, setTagConfig] = useState<MqttTagConfig | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        console.log('[Configuration] useEffect triggered - loading configuration');
        loadConfiguration();
    }, []);

    const loadConfiguration = async () => {
        console.log('[Configuration] loadConfiguration called');
        setLoading(true);
        setError(null);

        try {
            // Load broker config
            console.log('[Configuration] Fetching broker config...');
            const brokerResponse = await getBrokerConfig();
            console.log('[Configuration] Broker response:', brokerResponse);
            
            if (brokerResponse.success && brokerResponse.data) {
                setBrokerConfig(brokerResponse.data);
            } else if (!brokerResponse.success) {
                throw new Error(brokerResponse.error || 'Failed to load broker configuration');
            }

            // Load tag config
            console.log('[Configuration] Fetching tag config...');
            const tagResponse = await getTagConfig();
            console.log('[Configuration] Tag response:', tagResponse);
            
            if (tagResponse.success && tagResponse.data) {
                setTagConfig(tagResponse.data);
            } else if (!tagResponse.success) {
                throw new Error(tagResponse.error || 'Failed to load tag configuration');
            }

        } catch (err) {
            console.error('[Configuration] Error loading configuration:', err);
            setError(err instanceof Error ? err.message : 'Failed to load configuration');
        } finally {
            setLoading(false);
            console.log('[Configuration] loadConfiguration completed');
        }
    };

    const handleBrokerConfigSaved = (newConfig: MqttBrokerConfig) => {
        setBrokerConfig(newConfig);
    };

    const handleTagConfigSaved = (newConfig: MqttTagConfig) => {
        setTagConfig(newConfig);
    };

    if (loading) {
        console.log('[Configuration] Rendering loading state');
        return (
            <div className="mqtt-config-page">
                <div className="loading">Loading configuration...</div>
            </div>
        );
    }

    if (error) {
        console.log('[Configuration] Rendering error state:', error);
        return (
            <div className="mqtt-config-page">
                <div className="error-banner">
                    <strong>Error:</strong> {error}
                    <button onClick={loadConfiguration} className="btn-retry">Retry</button>
                </div>
            </div>
        );
    }

    console.log('[Configuration] Rendering main UI, activeTab:', activeTab);
    return (
        <div className="mqtt-config-page">
            <header className="page-header">
                <h1>MQTT UNS Publisher Configuration</h1>
                <p className="page-description">
                    Configure MQTT broker connection and tag publishing settings
                </p>
            </header>

            <div className="tabs">
                <button
                    className={`tab ${activeTab === 'broker' ? 'active' : ''}`}
                    onClick={() => setActiveTab('broker')}
                >
                    Broker Settings
                </button>
                <button
                    className={`tab ${activeTab === 'tags' ? 'active' : ''}`}
                    onClick={() => setActiveTab('tags')}
                >
                    Tag Publishing
                </button>
                <button
                    className={`tab ${activeTab === 'status' ? 'active' : ''}`}
                    onClick={() => setActiveTab('status')}
                >
                    Status & Statistics
                </button>
            </div>

            <div className="tab-content">
                {activeTab === 'broker' && (
                    <BrokerSettings
                        config={brokerConfig}
                        onConfigSaved={handleBrokerConfigSaved}
                    />
                )}
                {activeTab === 'tags' && (
                    <TagSelection
                        config={tagConfig}
                        onConfigSaved={handleTagConfigSaved}
                    />
                )}
                {activeTab === 'status' && (
                    <StatusDashboard />
                )}
            </div>
        </div>
    );
};

export default Configuration;

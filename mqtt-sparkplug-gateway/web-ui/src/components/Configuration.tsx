import React, { useEffect, useState } from 'react';
import { getPublishConfig } from '../api';
import { SparkplugPublishConfig } from '../types';
import BrokerSettings from './BrokerSettings';
import SparkplugConfig from './SparkplugConfig';
import '../styles.css';

type TabType = 'broker' | 'sparkplug';

const Configuration: React.FC = () => {
    const [activeTab, setActiveTab] = useState<TabType>('broker');
    const [publishConfig, setPublishConfig] = useState<SparkplugPublishConfig | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        loadConfiguration();
    }, []);

    const loadConfiguration = async () => {
        setLoading(true);
        setError(null);

        try {
            const response = await getPublishConfig();
            if (response.success && response.data) {
                const config = response.data[0] || null;
                setPublishConfig(config);
            } else if (!response.success) {
                throw new Error(response.error || 'Failed to load Sparkplug configuration');
            }
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to load configuration');
        } finally {
            setLoading(false);
        }
    };

    const handlePublishConfigSaved = (newConfig: SparkplugPublishConfig) => {
        setPublishConfig(newConfig);
    };

    if (loading) {
        return (
            <div className="mqtt-config-page">
                <div>Loading configuration...</div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="mqtt-config-page">
                <div className="message error">
                    <strong>Error:</strong> {error}
                    <button onClick={loadConfiguration} className="btn-secondary">Retry</button>
                </div>
            </div>
        );
    }

    return (
        <div className="mqtt-config-page">
            <header className="page-header">
                <h1>MQTT SparkplugB Publisher</h1>
                <p className="page-description">Configure brokers and SparkplugB tag mappings.</p>
            </header>

            <div className="tabs">
                <button
                    className={`tab ${activeTab === 'broker' ? 'active' : ''}`}
                    onClick={() => setActiveTab('broker')}
                >
                    Broker Configuration
                </button>
                <button
                    className={`tab ${activeTab === 'sparkplug' ? 'active' : ''}`}
                    onClick={() => setActiveTab('sparkplug')}
                >
                    Sparkplug Mapping
                </button>
            </div>

            <div className="tab-content">
                {activeTab === 'broker' && (
                    <BrokerSettings />
                )}
                {activeTab === 'sparkplug' && (
                    <SparkplugConfig
                        config={publishConfig}
                        onConfigSaved={handlePublishConfigSaved}
                    />
                )}
            </div>
        </div>
    );
};

export default Configuration;

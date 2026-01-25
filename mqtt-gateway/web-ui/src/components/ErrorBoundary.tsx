import React, { Component, ErrorInfo, ReactNode } from 'react';

interface Props {
    children: ReactNode;
}

interface State {
    hasError: boolean;
    error: Error | null;
    errorInfo: ErrorInfo | null;
}

class ErrorBoundary extends Component<Props, State> {
    constructor(props: Props) {
        super(props);
        this.state = {
            hasError: false,
            error: null,
            errorInfo: null
        };
    }

    static getDerivedStateFromError(error: Error): State {
        console.error('[ErrorBoundary] getDerivedStateFromError:', error);
        return {
            hasError: true,
            error,
            errorInfo: null
        };
    }

    componentDidCatch(error: Error, errorInfo: ErrorInfo) {
        console.error('[ErrorBoundary] componentDidCatch - Error:', error);
        console.error('[ErrorBoundary] componentDidCatch - Error message:', error.message);
        console.error('[ErrorBoundary] componentDidCatch - Error stack:', error.stack);
        console.error('[ErrorBoundary] componentDidCatch - Component stack:', errorInfo.componentStack);
        
        this.setState({
            error,
            errorInfo
        });
        
        // Store in window for debugging
        if (typeof window !== 'undefined') {
            (window as any).MQTT_LAST_ERROR = {
                error,
                errorInfo,
                timestamp: new Date().toISOString()
            };
            console.error('[ErrorBoundary] Error stored in window.MQTT_LAST_ERROR');
        }
    }

    render() {
        if (this.state.hasError) {
            console.error('[ErrorBoundary] Rendering error UI');
            return (
                <div style={{ padding: '20px', backgroundColor: '#fee', border: '2px solid #c00', borderRadius: '4px' }}>
                    <h2 style={{ color: '#c00', margin: '0 0 10px 0' }}>Component Error</h2>
                    <p><strong>Error:</strong> {this.state.error?.message || 'Unknown error'}</p>
                    <details style={{ marginTop: '10px' }}>
                        <summary style={{ cursor: 'pointer', fontWeight: 'bold' }}>Stack Trace</summary>
                        <pre style={{ 
                            marginTop: '10px', 
                            padding: '10px', 
                            backgroundColor: '#f5f5f5', 
                            overflow: 'auto',
                            fontSize: '12px'
                        }}>
                            {this.state.error?.stack}
                        </pre>
                    </details>
                    {this.state.errorInfo && (
                        <details style={{ marginTop: '10px' }}>
                            <summary style={{ cursor: 'pointer', fontWeight: 'bold' }}>Component Stack</summary>
                            <pre style={{ 
                                marginTop: '10px', 
                                padding: '10px', 
                                backgroundColor: '#f5f5f5', 
                                overflow: 'auto',
                                fontSize: '12px'
                            }}>
                                {this.state.errorInfo.componentStack}
                            </pre>
                        </details>
                    )}
                    <p style={{ marginTop: '10px', fontSize: '12px', color: '#666' }}>
                        Check console for full error details. Error also stored in window.MQTT_LAST_ERROR
                    </p>
                </div>
            );
        }

        return this.props.children;
    }
}

export default ErrorBoundary;

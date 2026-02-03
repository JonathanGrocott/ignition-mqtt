import React from 'react';
import ConfigurationComponent from './components/Configuration';
import ErrorBoundary from './components/ErrorBoundary';

export const Configuration: React.FC = () => (
    <ErrorBoundary>
        <ConfigurationComponent />
    </ErrorBoundary>
);

export default { Configuration };

import React from 'react';
import Header from './components/Header';
import List from './components/List'
import ConfigForm from './components/configForms/ConfigForm'

import "./styles/Layout.css"

const App: React.FC = () => {
  return (
    <div className="app-container">
      <Header />
      <List />
      <ConfigForm />
    </div>
  );
}

export default App;

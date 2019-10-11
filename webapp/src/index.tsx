import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from "mobx-react";

import './index.css';
import App from './App';
import * as serviceWorker from './serviceWorker';

import { ConfigStore } from "./stores/ConfigStore";
import ConfigService from './stores/services/ConfigService';

const url = window.location.origin

const configService = new ConfigService(url)
const configStore = new ConfigStore(configService)

ReactDOM.render(
    <Provider
        configStore={configStore}>
        <App />
    </Provider>,
    document.getElementById('root')
);

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: https://bit.ly/CRA-PWA
serviceWorker.unregister();
